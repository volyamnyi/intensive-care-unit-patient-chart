package com.superhumans.integration;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.superhumans.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");

    private final ObjectMapper mapper = new ObjectMapper();

    private List<AuditLogResponse> parseContent(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode content = root.get("content");
        return mapper.readValue(content.toString(),
                new TypeReference<List<AuditLogResponse>>() {});
    }

    @Test
    void getAllAuditLogs_returnsPaginated() throws Exception {
        var entity = authGet(getAdminToken());

        var res = restTemplate.exchange(
                "/api/audit", HttpMethod.GET, entity, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(parseContent(res.getBody())).isNotNull();
    }

    @Test
    void getAuditLogs_byEntityType_returnsFiltered() throws Exception {
        var entity = authGet(getAdminToken());

        var res = restTemplate.exchange(
                "/api/audit?entity=AUTH", HttpMethod.GET, entity, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        var logs = parseContent(res.getBody());
        if (!logs.isEmpty()) {
            assertThat(logs).allMatch(a -> "AUTH".equals(a.getEntity()));
        }
    }

    @Test
    void getAuditLogs_byAction_returnsFiltered() throws Exception {
        var entity = authGet(getAdminToken());

        var res = restTemplate.exchange(
                "/api/audit?action=LOGIN", HttpMethod.GET, entity, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        var logs = parseContent(res.getBody());
        if (!logs.isEmpty()) {
            assertThat(logs).allMatch(a -> "LOGIN".equals(a.getAction()));
        }
    }

    @Test
    void getAuditLogs_byDateRange_returnsFiltered() throws Exception {
        var entity = authGet(getAdminToken());

        LocalDateTime dateFrom = LocalDateTime.now().minusDays(7);
        LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

        var res = restTemplate.exchange(
                "/api/audit?dateFrom={from}&dateTo={to}",
                HttpMethod.GET, entity, String.class,
                dateFrom.toString(), dateTo.toString());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(parseContent(res.getBody())).isNotNull();
    }

    @Test
    void auditLogCreated_afterSignAction() throws Exception {
        SignRequest nurseReq = new SignRequest(13L, "audit-nurse-hash");
        var nurseEntity = authEntity(nurseReq, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, nurseEntity,
                Void.class, SEED_DAY_ID);

        var auditEntity = authGet(getAdminToken());
        var res = restTemplate.exchange(
                "/api/audit?action=SIGN_NURSE", HttpMethod.GET, auditEntity, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        var logs = parseContent(res.getBody());
        assertThat(logs).anyMatch(a -> SEED_DAY_ID.equals(a.getEntityId()));
    }
}
