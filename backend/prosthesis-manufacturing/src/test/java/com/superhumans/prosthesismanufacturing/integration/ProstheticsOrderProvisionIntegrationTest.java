package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.DocumentMisDTO;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
