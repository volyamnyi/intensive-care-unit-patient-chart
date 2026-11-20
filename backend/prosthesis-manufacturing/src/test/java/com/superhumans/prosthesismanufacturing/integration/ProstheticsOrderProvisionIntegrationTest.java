package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.prosthesismanufacturing.dto.OrderProvisionRequest;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.service.ProstheticsOrderService;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Setup step 2 → execution chain through the real Spring wiring: picking an
 * MIS limb-order document provisions the local patient + order rows
 * (idempotent), so selection never depends on pre-existing local data.
 * The MIS boundary is mocked; URL liveness is probed over real HTTP
 * against a local server. No external network.
 */
@SpringBootTest(properties = {"app.seed-data.enabled=false"})
@Transactional("prosthTransactionManager")
class ProstheticsOrderProvisionIntegrationTest {

    @Autowired
    private ProstheticsOrderService orderService;
    @Autowired
    private ProstheticsOrderRepository orderRepository;
    @Autowired
    private ProstheticsPatientRepository patientRepository;

    @MockitoBean
    private MisService misService;

    /**
     * Synthetic MIS patient for the #458 race test — free on the dev DB and
     * in every fresh CI DB; the lower-limb document is {@code 6002}.
     */
    private static final String RACE_PATIENT_ID = "999901";
    private static final Long RACE_PATIENT_ID_L = 999901L;
    private static final Long RACE_DOCUMENT_ID = 6002L;

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        }));
        server.createContext("/live", e -> send(e, 200));
        server.createContext("/dead", e -> send(e, 404));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void provision_createsPatientAndOrder_thenReusesThem() {
        when(misService.getPatientDocuments(13373L)).thenReturn(List.of(
                doc(681078L, 121L, baseUrl + "/live")));
        when(misService.getPatient(13373L)).thenReturn(Optional.empty());

        OrderProvisionRequest request = OrderProvisionRequest.builder()
                .patientId("13373").documentId(681078L).build();
        ProstheticsOrderResponse first = orderService.provisionFromMis(request);

        assertThat(first.getOrderNumber()).isEqualTo("MIS-13373-681078");
        assertThat(patientRepository.findById("13373")).isPresent();
        assertThat(orderRepository.findByOrderNumber("MIS-13373-681078")).isPresent();
        assertThat(orderRepository.findByOrderNumber("MIS-13373-681078").orElseThrow()
                        .getProductType())
                .isEqualTo(ProductType.LOWER_LIMB);

        long ordersBefore = orderRepository.count();
        ProstheticsOrderResponse second = orderService.provisionFromMis(request);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
    }

    /**
     * Issue #458 — two overlapping selections of one MIS document used to
     * pass the check-then-act window together; the loser of the unique-index
     * race then failed the whole transaction with the generic 409
     * "Data integrity violation" (duplicate key on
     * {@code uq_prosthetics_orders_number}). Both selections must now return
     * the same order id, with no duplicates.
     *
     * <p>Barrier mechanics: {@code getPatient} is the first call after the
     * {@code findByOrderNumber} check on the absent-patient path, so parking
     * both selections on a two-party barrier places them together in the
     * create section (both see the absent patient before either inserts).
     * The wait is bounded so the test cannot hang on the fixed code, where one
     * selection waits behind the other's advisory lock. Patient {@value #RACE_PATIENT_ID}
     * is synthetic — free on the dev DB and in every fresh CI DB — and the
     * {@code MIS-*} order number is unique for it.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void provisionFromMis_parallelSameDocument_returnsSameOrderId() throws Exception {
        when(misService.getPatientDocuments(RACE_PATIENT_ID_L)).thenReturn(List.of(
                doc(RACE_DOCUMENT_ID, 121L, baseUrl + "/live")));
        CyclicBarrier bothSelections = new CyclicBarrier(2);
        when(misService.getPatient(RACE_PATIENT_ID_L)).thenAnswer(invocation -> {
            try {
                bothSelections.await(3, TimeUnit.SECONDS);
            } catch (TimeoutException | java.util.concurrent.BrokenBarrierException expected) {
                // Fixed code: the sibling is blocked behind the other's advisory
                // lock — give the winner a bounded window and move on.
            }
            return Optional.of(PatientDTO.builder()
                    .id(RACE_PATIENT_ID_L).fullName("Бондаренко Тетяна Тестівна").build());
        });

        List<CompletableFuture<ProstheticsOrderResponse>> selections = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            selections.add(CompletableFuture.supplyAsync(() ->
                    orderService.provisionFromMis(OrderProvisionRequest.builder()
                            .patientId(RACE_PATIENT_ID).documentId(RACE_DOCUMENT_ID).build()),
                    ForkJoinPool.commonPool()));
        }

        ProstheticsOrderResponse first = selections.get(0).get(20, TimeUnit.SECONDS);
        ProstheticsOrderResponse second = selections.get(1).get(20, TimeUnit.SECONDS);
        assertThat(first.getOrderNumber()).isEqualTo("MIS-" + RACE_PATIENT_ID + "-" + RACE_DOCUMENT_ID);
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(orderRepository.findByOrderNumber("MIS-" + RACE_PATIENT_ID + "-" + RACE_DOCUMENT_ID)
                        .orElseThrow().getProductType())
                .isEqualTo(ProductType.LOWER_LIMB);
    }

    @AfterEach
    @Transactional
    void deleteRaceRows() {
        orderRepository.findByOrderNumber("MIS-" + RACE_PATIENT_ID + "-" + RACE_DOCUMENT_ID)
                .ifPresent(orderRepository::delete);
        patientRepository.deleteById(RACE_PATIENT_ID);
    }

    @Test
    void provision_deadUrl_throwsNotFoundWithoutSaving() {
        when(misService.getPatientDocuments(13373L)).thenReturn(List.of(
                doc(681078L, 121L, baseUrl + "/dead")));

        assertThatThrownBy(() -> orderService.provisionFromMis(OrderProvisionRequest.builder()
                        .patientId("13373").documentId(681078L).build()))
                .isInstanceOf(NotFoundException.class);
        assertThat(orderRepository.findByOrderNumber("MIS-13373-681078")).isEmpty();
    }

    private static DocumentMisDTO doc(long documentId, long templateId, String url) {
        return DocumentMisDTO.builder()
                .documentId(documentId)
                .documentTemplateId(templateId)
                .documentTemplateName("Замовлення на протези нижніх кінцівок")
                .documentUrl(url)
                .patientId(13373L)
                .patientFullName("Бондаренко Тетяна Тестівна")
                .orderDate(LocalDateTime.of(2026, 9, 7, 0, 0, 0))
                .build();
    }

    private static void send(HttpExchange exchange, int status) throws IOException {
        byte[] body = "x".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
