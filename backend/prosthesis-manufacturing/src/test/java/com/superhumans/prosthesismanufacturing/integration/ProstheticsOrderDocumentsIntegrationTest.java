package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.prosthesismanufacturing.service.ProstheticsOrderService;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;

/**
 * Setup step 2 — MIS limb-order documents through the real Spring wiring
 * ({@link ProstheticsOrderService} + {@link com.superhumans.prosthesismanufacturing.service.DocumentUrlAvailability}).
 * The MIS boundary is mocked; URL liveness is probed over real HTTP against
 * a local server (200 kept, 404 excluded). No external network.
 */
@SpringBootTest(properties = {"app.seed-data.enabled=false"})
class ProstheticsOrderDocumentsIntegrationTest {

    @Autowired
    private ProstheticsOrderService orderService;

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
    void documentsForPatient_filtersTemplatesAndDrops404() {
        when(misService.getPatientDocuments(13373L)).thenReturn(List.of(
                doc(121L, baseUrl + "/live", "Замовлення на протези нижніх кінцівок"),
                doc(120L, baseUrl + "/live", "Замовлення на протези верхніх кінцівок"),
                doc(121L, baseUrl + "/dead", "Замовлення на протези нижніх кінцівок"),
                doc(999L, baseUrl + "/live", "Інший документ"),
                doc(121L, "  ", "Замовлення на протези нижніх кінцівок")));

        List<DocumentMisDTO> res = orderService.getAllLowerLimbsOrdersForByPatientId("13373");

        assertThat(res).extracting(DocumentMisDTO::getDocumentTemplateName)
                .containsExactly(
                        "Замовлення на протези нижніх кінцівок",
                        "Замовлення на протези верхніх кінцівок");
        assertThat(res).extracting(DocumentMisDTO::getDocumentUrl)
                .allMatch(url -> url.startsWith(baseUrl + "/live"));
    }

    @Test
    void preservesOrderNumber_endToEnd() {
        when(misService.getPatientDocuments(13373L)).thenReturn(List.of(
                DocumentMisDTO.builder()
                        .documentId(681078L)
                        .documentTemplateId(121L)
                        .documentTemplateName("Замовлення на протези нижніх кінцівок")
                        .documentUrl(baseUrl + "/live")
                        .patientId(13373L)
                        .orderNumber("BZ-2026-0042")
                        .build()));

        List<DocumentMisDTO> res = orderService.getAllLowerLimbsOrdersForByPatientId("13373");

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getOrderNumber()).isEqualTo("BZ-2026-0042");
    }

    private static DocumentMisDTO doc(long templateId, String url, String templateName) {
        return DocumentMisDTO.builder()
                .documentId(templateId)
                .documentTemplateId(templateId)
                .documentTemplateName(templateName)
                .documentUrl(url)
                .patientId(13373L)
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
