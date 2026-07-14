package com.superhumans.integration;

import com.superhumans.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");

    @Test
    void getAllAuditLogs_returnsPaginated() {
        var entity = authGet(getAdminToken());

        var res = restTemplate.exchange(
                "/api/audit", HttpMethod.GET, entity,
                new ParameterizedTypeReference<Page<AuditLogResponse>>() {});

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
    }

    @Test
    void getAuditLogs_byEntityType_returnsFiltered() {
        var entity = authGet(getAdminToken());

        var res = restTemplate.exchange(
                "/api/audit?entity=AUTH", HttpMethod.GET, entity,
                new ParameterizedTypeReference<Page<AuditLogResponse>>() {});

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        if (!res.getBody().isEmpty()) {
            assertThat(res.getBody().getContent()).allMatch(
                    a -> "AUTH".equals(a.getEntity()));
        }
    }

    @Test
    void getAuditLogs_byAction_returnsFiltered() {
        var entity = authGet(getAdminToken());

        var res = restTemplate.exchange(
                "/api/audit?action=LOGIN", HttpMethod.GET, entity,
                new ParameterizedTypeReference<Page<AuditLogResponse>>() {});

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        if (!res.getBody().isEmpty()) {
            assertThat(res.getBody().getContent()).allMatch(
                    a -> "LOGIN".equals(a.getAction()));
        }
    }

    @Test
    void getAuditLogs_byDateRange_returnsFiltered() {
        var entity = authGet(getAdminToken());

        LocalDateTime dateFrom = LocalDateTime.now().minusDays(7);
        LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

        var res = restTemplate.exchange(
                "/api/audit?dateFrom={from}&dateTo={to}",
                HttpMethod.GET, entity,
                new ParameterizedTypeReference<Page<AuditLogResponse>>() {},
                dateFrom.toString(), dateTo.toString());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
    }

    @Test
    void auditLogCreated_afterSignAction() {
        SignRequest nurseReq = new SignRequest(UUID.randomUUID(), "audit-nurse-hash");
        var nurseEntity = authEntity(nurseReq, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, nurseEntity,
                Void.class, SEED_DAY_ID);

        var auditEntity = authGet(getAdminToken());
        var res = restTemplate.exchange(
                "/api/audit?action=SIGN_NURSE", HttpMethod.GET, auditEntity,
                new ParameterizedTypeReference<Page<AuditLogResponse>>() {});

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getContent())
                .anyMatch(a -> SEED_DAY_ID.equals(a.getEntityId()));
    }
}
