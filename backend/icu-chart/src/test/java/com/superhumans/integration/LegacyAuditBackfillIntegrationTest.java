package com.superhumans.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.superhumans.entity.core.AuditLog;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditService;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyAuditBackfillIntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String RUN_TAG = UUID.randomUUID().toString().substring(0, 8);
    private static final AtomicInteger METHOD_SEQ = new AtomicInteger();

    @Autowired
    AuditService auditService;

    @Autowired
    AuditLogRepository auditLogRepository;

    @Autowired
    @Qualifier("coreTransactionManager")
    PlatformTransactionManager coreTransactionManager;

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalStateException("Response is not JSON", exception);
        }
    }

    private JsonNode postBackfill(String token) {
        var res = restTemplate.exchange("/api/admin/audit/backfill", HttpMethod.POST,
                authGet(token), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        return parse(res.getBody());
    }

    private JsonNode searchLegacy(String targetType) {
        var res = restTemplate.exchange(
                "/api/audit/events?module={module}&targetType={type}",
                HttpMethod.GET, authGet(getAdminToken()), String.class,
                "legacy", targetType);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return parse(res.getBody());
    }

    private JsonNode detail(String auditId) {
        var res = restTemplate.exchange("/api/audit/events/{id}", HttpMethod.GET,
                authGet(getAdminToken()), String.class, auditId);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return parse(res.getBody());
    }

    @Test
    void backfill_copiesRowsWithKindsAndVerifies() {
        String entity = "BackfillProbe-" + RUN_TAG + "-" + METHOD_SEQ.incrementAndGet();
        UUID itemId = UUID.randomUUID();
        auditService.logEvent(entity, itemId, "CREATE", 11L, null, "probe-new");
        auditService.logEvent(entity, itemId, "API_PROBE_SEARCH", 11L, null, null);
        new TransactionTemplate(coreTransactionManager).execute(status -> {
            auditLogRepository.save(AuditLog.builder()
                    .entity(entity)
                    .entityId(UUID.randomUUID())
                    .action("UPDATE")
                    .userId(11L)
                    .oldValue("probe-old")
                    .isDeleted(true)
                    .timestamp(LocalDateTime.now().minusDays(3))
                    .build());
            return null;
        });

        JsonNode report = postBackfill(getAdminToken());
        assertThat(report.get("inserted").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(report.get("verified").asBoolean()).isTrue();
        assertThat(report.get("checksumMismatches").size()).isZero();

        JsonNode page = searchLegacy(entity);
        assertThat(page.get("totalElements").asLong()).isEqualTo(3);
        boolean hasHttpKind = false;
        boolean hasDeleted = false;
        for (JsonNode node : page.get("content")) {
            assertThat(node.get("legacy").asBoolean()).isTrue();
            hasHttpKind |= "legacy.http.request".equals(node.get("action").asText());
            JsonNode legacy = detail(node.get("auditId").asText()).get("legacyDetail");
            assertThat(legacy.get("sourceTable").asText()).isEqualTo("audit_logs");
            assertThat(legacy.get("legacyEntity").asText()).isEqualTo(entity);
            assertThat(legacy.get("schemaVersion").asInt()).isZero();
            assertThat(legacy.get("contextCompleteness").asText()).isEqualTo("LEGACY");
            assertThat(legacy.get("timestampPrecision").asText()).isEqualTo("LEGACY_NAIVE");
            assertThat(legacy.get("outcome").asText()).isEqualTo("UNKNOWN_LEGACY");
            assertThat(legacy.get("checksum").asText()).hasSize(64);
            if ("UPDATE".equals(legacy.get("legacyAction").asText())) {
                assertThat(legacy.get("legacyOldValue").asText()).isEqualTo("probe-old");
                assertThat(legacy.get("legacyIsDeleted").asBoolean()).isTrue();
                hasDeleted = true;
            }
            if ("CREATE".equals(legacy.get("legacyAction").asText())) {
                assertThat(legacy.get("legacyNewValue").asText()).isEqualTo("probe-new");
            }
        }
        assertThat(hasHttpKind).isTrue();
        assertThat(hasDeleted).isTrue();

        JsonNode anyDetail = detail(page.get("content").get(0).get("auditId").asText());
        assertThat(anyDetail.get("integrityVerified").asBoolean()).isTrue();
    }

    @Test
    void backfill_isIdempotentAcrossRuns() {
        String entity = "BackfillProbe-" + RUN_TAG + "-" + METHOD_SEQ.incrementAndGet();
        auditService.logEvent(entity, UUID.randomUUID(), "CREATE", 11L, null, null);

        JsonNode first = postBackfill(getAdminToken());
        assertThat(first.get("verified").asBoolean()).isTrue();

        JsonNode second = postBackfill(getAdminToken());
        assertThat(second.get("verified").asBoolean()).isTrue();
        assertThat(second.get("checksumMismatches").size()).isZero();

        // Exactly one legacy copy despite two runs — no duplicates.
        assertThat(searchLegacy(entity).get("totalElements").asLong()).isEqualTo(1);
    }

    @Test
    void backfill_requiresAdmin() {
        var res = restTemplate.exchange("/api/admin/audit/backfill", HttpMethod.POST,
                authGet(getNurseToken()), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
