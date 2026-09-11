package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.service.DocumentUrlAvailability;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * HTTP security contract for the production monitoring API (manufacturing
 * epic #271, issue #274): permission-gated list/detail/team, own-only scope
 * without {@code VIEW_ALL}, and patient masking without
 * {@code PATIENT_VIEW} — over the real filter chain.
 *
 * <p>Prosthetics rows are created and removed per test (the shared
 * integration databases carry no prosthetics seed); the runtime matrix is
 * left untouched.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductionSecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE = "/api/prosthesis-manufacturing/production";

    @Autowired private FlowInstanceRepository instanceRepository;
    @Autowired private ProstheticsOrderRepository orderRepository;
    @Autowired private ProstheticsPatientRepository patientRepository;
    @Autowired private FlowTemplateRepository templateRepository;
    @Autowired private UserRepository userRepository;

    @MockitoBean private DocumentUrlAvailability documentUrlAvailability;

    private static final String PASSWORD = "test123";
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final List<UUID> instanceIds = new ArrayList<>();
    private final List<UUID> orderIds = new ArrayList<>();
    private final List<String> patientIds = new ArrayList<>();
    private final List<UUID> templateIds = new ArrayList<>();

    private Long prosthetist1Id;
    private Long prosthetist2Id;
    private UUID ownInstanceId;
    private UUID foreignInstanceId;

    @BeforeAll
    void seedUsers() {
        // data-test-core.sql carries no prosthetics users: provision them once
        // per class (BCrypt-hashed at seed time, removed in cleanUpUsers).
        // Per-method creation would advance the users id sequence into the
        // fixed seed ids (11-17) after TRUNCATE ... RESTART IDENTITY.
        createUser("prod_prosthetist1", UserRole.PROSTHETIST);
        createUser("prod_prosthetist2", UserRole.PROSTHETIST);
        createUser("prod_prosthadmn", UserRole.PROSTHETICS_ADMINISTRATOR);
    }

    @AfterAll
    void cleanUpUsers() {
        userRepository.findByLogin("prod_prosthetist1").ifPresent(userRepository::delete);
        userRepository.findByLogin("prod_prosthetist2").ifPresent(userRepository::delete);
        userRepository.findByLogin("prod_prosthadmn").ifPresent(userRepository::delete);
    }

    @BeforeEach
    void seed() {
        prosthetist1Id = loginUserId("prod_prosthetist1", PASSWORD);
        prosthetist2Id = loginUserId("prod_prosthetist2", PASSWORD);

        FlowTemplate template = templateRepository.save(FlowTemplate.builder()
                .name("TP-SEC-" + UUID.randomUUID().toString().substring(0, 8))
                .templateVersion(1)
                .productType(ProductType.LOWER_LIMB)
                .status(TemplateStatus.ACTIVE)
                .build());
        templateIds.add(template.getId());

        ProstheticsPatient patient = patientRepository.save(ProstheticsPatient.builder()
                // Unique digits-only id per run: never collide with dev leftovers.
                .id("9" + String.format("%05d",
                        java.util.concurrent.ThreadLocalRandom.current().nextInt(100000)))
                .pib("Пацієнт Безпеки")
                .birthDate(LocalDate.of(1990, 5, 5))
                .gender("Чоловіча")
                .build());
        patientIds.add(patient.getId());
        long numericPatientId = Long.parseLong(patient.getId());
        ProstheticsOrder ownOrder = saveOrder(patient, "MIS-" + patient.getId() + "-55");
        ProstheticsOrder foreignOrder = saveOrder(patient, "MIS-" + patient.getId() + "-77");
        ownInstanceId = saveInstance(template.getId(), ownOrder.getId(), patient.getId(), prosthetist1Id);
        foreignInstanceId =
                saveInstance(template.getId(), foreignOrder.getId(), patient.getId(), prosthetist2Id);

        when(misService.getPatientDocuments(numericPatientId)).thenReturn(List.of(
                DocumentMisDTO.builder().documentId(55L).documentTemplateId(121L)
                        .documentUrl("https://mis.local/55").patientFullName("Пацієнт Безпеки")
                        .build()));
        when(documentUrlAvailability.isAvailable(anyString())).thenReturn(true);
    }

    @AfterEach
    void cleanUp() {
        instanceIds.forEach(instanceRepository::deleteById);
        orderIds.forEach(orderRepository::deleteById);
        patientIds.forEach(patientRepository::deleteById);
        templateIds.forEach(templateRepository::deleteById);
        instanceIds.clear();
        orderIds.clear();
        patientIds.clear();
        templateIds.clear();
    }

    private void createUser(String login, UserRole role) {
        userRepository.findByLogin(login).ifPresent(userRepository::delete);
        userRepository.save(User.builder()
                .login(login)
                .passwordHash(encoder.encode(PASSWORD))
                .fullName(login)
                .role(role)
                .build());
    }

    @Test
    void unauthenticated_isUnauthorized() {
        ResponseEntity<String> res = restTemplate.exchange(
                BASE, HttpMethod.GET, new HttpEntity<>((Void) null), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void nurse_withoutView_isForbiddenEverywhere() {
        String nurse = getNurseToken();

        assertThat(get(BASE, nurse).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(BASE + "/team", nurse).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(BASE + "/" + ownInstanceId, nurse).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void prosthetist_seesOwnItemsOnly() {
        String token = loginAs("prod_prosthetist1", PASSWORD);

        JsonNode body = get(BASE, token).getBody();
        assertThat(body.get("content").isArray()).isTrue();
        assertThat(body.get("content").size()).isGreaterThanOrEqualTo(1);
        body.get("content").forEach(row ->
                assertThat(row.get("prosthetistUserId").asLong()).isEqualTo(prosthetist1Id));

        // The assignee filter is forced to self without VIEW_ALL.
        JsonNode forced = get(BASE + "?assigneeId=" + prosthetist2Id, token).getBody();
        forced.get("content").forEach(row ->
                assertThat(row.get("prosthetistUserId").asLong()).isEqualTo(prosthetist1Id));
    }

    @Test
    void prosthetist_foreignDetail_isNotFound_ownDetail_isMasked() {
        String token = loginAs("prod_prosthetist1", PASSWORD);

        assertThat(get(BASE + "/" + foreignInstanceId, token).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<JsonNode> own = get(BASE + "/" + ownInstanceId, token);
        assertThat(own.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(own.getBody().get("workItem").get("instanceId").asText())
                .isEqualTo(ownInstanceId.toString());
        // Masked without PATIENT_VIEW: PIB only, no details, no documents.
        assertThat(own.getBody().get("patient").get("pib").asText()).isEqualTo("Пацієнт Безпеки");
        assertThat(own.getBody().get("patient").path("birthDate").isNull()
                || own.getBody().get("patient").path("birthDate").isMissingNode()).isTrue();
        assertThat(own.getBody().get("patientDetailsVisible").asBoolean()).isFalse();
        assertThat(own.getBody().get("documents").size()).isZero();
    }

    @Test
    void prosthetist_team_isForbidden_adminAndHod_mayRead() {
        String prosthetist = loginAs("prod_prosthetist1", PASSWORD);
        assertThat(get(BASE + "/team", prosthetist).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String admin = loginAs("prod_prosthadmn", PASSWORD);
        ResponseEntity<JsonNode> team = get(BASE + "/team", admin);
        assertThat(team.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(team.getBody().isArray()).isTrue();

        String hod = getHodToken();
        assertThat(get(BASE + "/team", hod).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void admin_detail_isFull() {
        String admin = loginAs("prod_prosthadmn", PASSWORD);

        ResponseEntity<JsonNode> res = get(BASE + "/" + ownInstanceId, admin);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("patientDetailsVisible").asBoolean()).isTrue();
        assertThat(res.getBody().get("patient").get("birthDate").asText())
                .isEqualTo("1990-05-05");
        assertThat(res.getBody().get("documents").size()).isEqualTo(1);
        assertThat(res.getBody().get("matchedDocument").get("documentId").asLong())
                .isEqualTo(55L);
        assertThat(res.getBody().get("documentsUnknown").asBoolean()).isFalse();
        assertThat(res.getBody().get("timeline").isArray()).isTrue();
    }

    @Test
    void admin_unknownDetail_isNotFound() {
        String admin = loginAs("prod_prosthadmn", PASSWORD);

        assertThat(get(BASE + "/" + UUID.randomUUID(), admin).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void normative_adminRoundtrips_prosthetistIsForbidden() throws Exception {
        String admin = loginAs("prod_prosthadmn", PASSWORD);
        String prosthetist = loginAs("prod_prosthetist1", PASSWORD);

        assertThat(get(BASE + "/settings/normative", prosthetist).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<JsonNode> before = get(BASE + "/settings/normative", admin);
        assertThat(before.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(before.getBody().get("overdueMultiplier").asDouble()).isPositive();
        assertThat(before.getBody().get("staleDays").asInt()).isPositive();

        assertThat(putJson(BASE + "/settings/normative", admin,
                Map.of("overdueMultiplier", 2.0, "staleDays", 3)).getBody()
                .get("overdueMultiplier").asDouble()).isEqualTo(2.0);
        assertThat(putJson(BASE + "/settings/normative", admin,
                Map.of("overdueMultiplier", 2.0, "staleDays", 3)).getBody()
                .get("staleDays").asInt()).isEqualTo(3);

        assertThat(putRaw(BASE + "/settings/normative", admin,
                Map.of("overdueMultiplier", 0.5, "staleDays", 3)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Restore defaults for other tests (shared database).
        assertThat(putRaw(BASE + "/settings/normative", admin,
                Map.of("overdueMultiplier", 1.5, "staleDays", 7)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private ProstheticsOrder saveOrder(ProstheticsPatient patient, String orderNumber) {
        ProstheticsOrder order = orderRepository.save(ProstheticsOrder.builder()
                .orderNumber(orderNumber)
                .patient(patient)
                .productType(ProductType.LOWER_LIMB)
                .limbSide(LimbSide.LEFT)
                .status(OrderStatus.NEW)
                .build());
        orderIds.add(order.getId());
        return order;
    }

    private UUID saveInstance(UUID templateId, UUID orderId, String patientId, Long assignee) {
        FlowInstance instance = instanceRepository.save(FlowInstance.builder()
                .templateId(templateId)
                .patientId(patientId)
                .orderId(orderId)
                .assignedUserId(assignee)
                .status(FlowInstanceStatus.IN_PROGRESS)
                .startTime(LocalDateTime.now().minusHours(1))
                .totalActiveSeconds(0L)
                .totalIdleSeconds(0L)
                .branchSequence(1)
                .build());
        instanceIds.add(instance.getId());
        return instance.getId();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseEntity<JsonNode> get(String url, String token) {
        ResponseEntity<String> res =
                restTemplate.exchange(url, HttpMethod.GET, authGet(token), String.class);
        return withJsonBody(res);
    }

    private ResponseEntity<JsonNode> putJson(String url, String token, Map<String, Object> body)
            throws Exception {
        return withJsonBody(putRaw(url, token, body));
    }

    private ResponseEntity<String> putRaw(String url, String token, Map<String, Object> body)
            throws Exception {
        String json = objectMapper.writeValueAsString(body);
        org.springframework.http.HttpHeaders headers = authHeaders(token);
        headers.set("Content-Type", "application/json");
        return restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(json, headers), String.class);
    }

    private ResponseEntity<JsonNode> withJsonBody(ResponseEntity<String> res) {
        JsonNode body = null;
        if (res.getBody() != null) {
            try {
                body = objectMapper.readTree(res.getBody());
            } catch (Exception e) {
                throw new IllegalStateException("Response is not JSON", e);
            }
        }
        return new ResponseEntity<>(body, res.getHeaders(), res.getStatusCode());
    }
}
