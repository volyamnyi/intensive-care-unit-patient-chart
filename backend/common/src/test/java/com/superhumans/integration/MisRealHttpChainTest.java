package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.superhumans.mis.MisApiClient;
import com.superhumans.mis.MisApiException;
import com.superhumans.mis.MisApiProperties;
import com.superhumans.mis.MisAuthException;
import com.superhumans.mis.MisAuthService;
import com.superhumans.mis.MisServiceImpl;
import com.superhumans.mis.MisTimeoutException;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.service.AuditService;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Real-HTTP chain test (issue #266) for the MIS transport stack.
 *
 * <p>Spins up a JDK HttpServer speaking the live envelope contract (a
 * form-encoded token call plus JSON procedure envelopes) and drives the
 * production stack end to end with real objects only: a real
 * {@link MisApiProperties}, {@link MisAuthService}, {@link MisApiClient} and
 * {@link MisServiceImpl}. Only the audit sink is a Mockito double. No Spring
 * context, no credentials, no external network.
 */
class MisRealHttpChainTest {

    private static final String LOGIN = "chain-login";
    private static final String PASSWORD = "chain-pw-CANARY-7f3a";
    private static final String GUID = "chain-guid-1234";
    private static final String TOKEN = "chain-access-token";

    private static final String TOKEN_JSON = "{\"access_token\":\"" + TOKEN
            + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}";

    private static final String PATIENT_ENVELOPE = """
            {"spiPatientProsthesCheck":[
              {"id":900001,"fullName":"Snihko Ivan Petrovych","birthDate":"1991-03-14T00:00:00",
               "sexCode":"MAL","departmentId":19,"room":"411A-Тестова","bed":"Ліжко №1",
               "doctorName":"Ямний В. М."}
            ]}""";

    private static final String MEDICINE_ENVELOPE = """
            {"medicineItemKindDetails":[
              {"itemKindID":101,"itemKindName":"Paracetamol 500 mg",
               "itemKindIsDisabled":false}
            ]}""";

    private static final String DOCUMENT_ENVELOPE = """
            {"spiDocumentProsthesCheck":[
              {"documentTemplateID":120,"documentName":"Prosthetic Order"}
            ]}""";

    private HttpServer server;
    private String baseUrl;
    private AuditService audit;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<RecordedRequest> tokenHits = new CopyOnWriteArrayList<>();
    private final List<RecordedRequest> runHits = new CopyOnWriteArrayList<>();
    private final AtomicInteger runCalls = new AtomicInteger();
    private volatile int runStatus = 200;
    private volatile String runBody = PATIENT_ENVELOPE;
    private volatile String tokenBody = TOKEN_JSON;
    private volatile long runDelayMs = 0;
    private volatile boolean failFirstRunWith401 = false;

    @BeforeEach
    void startServer() throws IOException {
        tokenHits.clear();
        runHits.clear();
        runCalls.set(0);
        runStatus = 200;
        runBody = PATIENT_ENVELOPE;
        tokenBody = TOKEN_JSON;
        runDelayMs = 0;
        failFirstRunWith401 = false;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        }));
        server.createContext("/token", this::handleToken);
        server.createContext("/api/run", this::handleRun);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void patientChain_endToEndOverRealHttp() throws Exception {
        MisServiceImpl service = newService();

        List<PatientDTO> patients = service.getAllPatientsUnderTreatment();

        assertThat(patients).hasSize(1);
        assertThat(patients.get(0).getFullName()).isEqualTo("Snihko Ivan Petrovych");
        assertThat(patients.get(0).getDepartmentId()).isEqualTo(19L);
        verify(audit).logAction(eq("MIS"), isNull(), eq("GET_ALL_PATIENTS"), isNull());

        assertThat(tokenHits).hasSize(1);
        RecordedRequest token = tokenHits.get(0);
        assertThat(token.contentType).startsWith("application/x-www-form-urlencoded");
        assertThat(token.body).contains("grant_type=password");
        assertThat(token.body).contains("username=" + LOGIN);
        assertThat(token.body).contains(GUID);
        assertThat(token.body).contains("password=" + PASSWORD);

        assertThat(runHits).hasSize(1);
        RecordedRequest run = runHits.get(0);
        assertThat(run.auth).isEqualTo("Bearer " + TOKEN);
        JsonNode runJson = mapper.readTree(run.body);
        assertThat(runJson.get("name").asText()).isEqualTo("spiPatientProsthesCheck");
        assertThat(runJson.get("installationId").asText()).isEqualTo(GUID);
        assertThat(hasParam(runJson, "Login", LOGIN)).isTrue();
    }

    @Test
    void medicineChain_endToEndOverRealHttp() {
        runBody = MEDICINE_ENVELOPE;
        MisServiceImpl service = newService();

        List<MedicineMisDTO> catalog = service.searchMedicineCatalog(null);

        assertThat(catalog).hasSize(1);
        assertThat(catalog.get(0).getName()).isEqualTo("Paracetamol 500 mg");
        assertThat(catalog.get(0).getItemKindIsDisabled()).isFalse();
        assertThat(tokenHits).hasSize(1);
        assertThat(runHits).hasSize(1);
    }

    @Test
    void documentChain_sendsPatientIdParam() throws Exception {
        runBody = DOCUMENT_ENVELOPE;
        MisServiceImpl service = newService();

        List<DocumentMisDTO> docs = service.getPatientDocuments(900001L);

        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getDocumentTemplateId()).isEqualTo(120L);
        JsonNode runJson = mapper.readTree(runHits.get(0).body);
        assertThat(runJson.get("name").asText()).isEqualTo("spiDocumentProsthesCheck");
        assertThat(hasParam(runJson, "PatientID", "900001")).isTrue();
    }

    @Test
    void tokenCachedAcrossCalls_singleTokenFetch() {
        MisServiceImpl service = newService();

        service.getAllPatientsUnderTreatment();
        service.getAllPatientsUnderTreatment();

        assertThat(tokenHits).hasSize(1);
        assertThat(runHits).hasSize(2);
    }

    @Test
    void http401_triggersSingleReauthAndRetry() {
        failFirstRunWith401 = true;
        MisServiceImpl service = newService();

        List<PatientDTO> patients = service.getAllPatientsUnderTreatment();

        assertThat(patients).hasSize(1);
        assertThat(runCalls.get()).isEqualTo(2);
        assertThat(tokenHits).hasSize(2);
    }

    @Test
    void runReadTimeout_throwsMisTimeoutException() {
        runDelayMs = 1500;
        MisServiceImpl service = newService(baseUrl, 1000, 300);

        Throwable thrown = catchThrowable(service::getAllPatientsUnderTreatment);

        assertThat(thrown).isInstanceOf(MisTimeoutException.class);
    }

    @Test
    void unreachableServer_throwsMisAuthExceptionWithoutSecrets() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        MisServiceImpl service = newService("http://127.0.0.1:" + closedPort, 1000, 1000);

        Throwable thrown = catchThrowable(service::getAllPatientsUnderTreatment);

        assertThat(thrown).isInstanceOf(MisAuthException.class);
        assertThat(thrown.getMessage()).doesNotContain(PASSWORD);
    }

    @Test
    void malformedRunBody_throwsMisApiExceptionWithoutSecrets() {
        runBody = "not-json{{{";
        MisServiceImpl service = newService();

        Throwable thrown = catchThrowable(service::getAllPatientsUnderTreatment);

        assertThat(thrown).isExactlyInstanceOf(MisApiException.class);
        assertThat(thrown.getMessage()).startsWith("MIS API call failed: spiPatientProsthesCheck");
        assertThat(thrown.getMessage()).doesNotContain(PASSWORD);
    }

    @Test
    void missingBaseUrl_failFastNamesKeysNotValues() {
        MisServiceImpl service = newService("", 2000, 2000);

        Throwable thrown = catchThrowable(() -> service.searchMedicineCatalog(null));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(thrown.getMessage()).contains("app.mis.api.base-url");
        assertThat(thrown.getMessage()).doesNotContain(PASSWORD);
    }

    @Test
    void tokenMissingInResponse_throwsMisAuthException() {
        tokenBody = "{}";
        MisServiceImpl service = newService();

        Throwable thrown = catchThrowable(service::getAllPatientsUnderTreatment);

        assertThat(thrown).isInstanceOf(MisAuthException.class);
        assertThat(thrown.getMessage()).doesNotContain(PASSWORD);
    }

    private MisServiceImpl newService() {
        return newService(baseUrl, 2000, 2000);
    }

    private MisServiceImpl newService(String base, int connectMs, int readMs) {
        MisApiProperties props = new MisApiProperties();
        ReflectionTestUtils.setField(props, "baseUrl", base);
        ReflectionTestUtils.setField(props, "tokenPath", "/token");
        ReflectionTestUtils.setField(props, "runPath", "/api/run");
        ReflectionTestUtils.setField(props, "login", LOGIN);
        ReflectionTestUtils.setField(props, "password", PASSWORD);
        ReflectionTestUtils.setField(props, "installationGuid", GUID);
        ReflectionTestUtils.setField(props, "connectTimeoutMs", connectMs);
        ReflectionTestUtils.setField(props, "readTimeoutMs", readMs);
        ReflectionTestUtils.setField(props, "tokenSkewSec", 60);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectMs));
        factory.setReadTimeout(Duration.ofMillis(readMs));
        RestTemplate restTemplate = new RestTemplate(factory);
        ObjectMapper objectMapper = new ObjectMapper();
        MisAuthService authService = new MisAuthService(restTemplate, objectMapper, props);
        MisApiClient client = new MisApiClient(restTemplate, objectMapper, authService, props);
        audit = mock(AuditService.class);
        return new MisServiceImpl(client, audit);
    }

    private void handleToken(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        tokenHits.add(record(exchange, body));
        sendJson(exchange, 200, tokenBody);
    }

    private void handleRun(HttpExchange exchange) throws IOException {
        int call = runCalls.incrementAndGet();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (failFirstRunWith401 && call == 1) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }
        if (runDelayMs > 0) {
            try {
                Thread.sleep(runDelayMs);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        runHits.add(record(exchange, body));
        sendJson(exchange, runStatus, runBody);
    }

    private RecordedRequest record(HttpExchange exchange, String body) {
        return new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("Content-Type"),
                body);
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private boolean hasParam(JsonNode runJson, String name, String value) {
        for (JsonNode param : runJson.get("params")) {
            if (name.equals(param.get("name").asText())
                    && value.equals(param.get("value").asText())) {
                return true;
            }
        }
        return false;
    }

    private static final class RecordedRequest {
        final String method;
        final String path;
        final String auth;
        final String contentType;
        final String body;

        RecordedRequest(String method, String path, String auth, String contentType, String body) {
            this.method = method;
            this.path = path;
            this.auth = auth;
            this.contentType = contentType;
            this.body = body;
        }
    }
}
