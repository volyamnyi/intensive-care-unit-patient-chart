package com.superhumans.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.superhumans.audit.AuditActionDefinition;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventRecorder;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventQueryIntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    AuditEventRecorder auditEventRecorder;

    /**
     * Per-run tag: scratch DBs persist across runs and {@code audit_events}
     * is append-only (never truncated by seed scripts), so fixed synthetic
     * keys would collide with previous runs' events.
     */
    private static final String RUN_TAG = UUID.randomUUID().toString().substring(0, 8);

    private static String syntheticLogin() {
        return "synthetic-auditor-" + RUN_TAG;
    }

    private static String syntheticUserId() {
        return "99-" + RUN_TAG;
    }

    private JsonNode getJson(String url, String token, Object... vars) {
        var res = restTemplate.exchange(url, HttpMethod.GET, authGet(token), String.class, vars);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        try {
            return mapper.readTree(res.getBody());
        } catch (Exception exception) {
            throw new IllegalStateException("Response is not JSON", exception);
        }
    }

    private AuditEvent.AuditActor userActor(String login) {
        return new AuditEvent.AuditActor(AuditEvent.ActorType.USER, syntheticUserId(), login, login,
                Set.of("ADMINISTRATOR"), null, null);
    }

    private void recordGrant(UUID auditId, Instant occurredAt, UUID parentAuditId) {
        auditEventRecorder.record(AuditEvent.builder()
                .auditId(auditId)
                .actor(userActor(syntheticLogin()))
                .eventClass(AuditActionDefinition.EventClass.SECURITY)
                .module("platform")
                .functionalArea("rbac")
                .action("platform.rbac.permission.grant")
                .actionType(AuditActionDefinition.ActionType.PERMISSION_GRANT)
                .target(new AuditEvent.AuditTarget("User", syntheticUserId(), null))
                .changes(List.of(new AuditEvent.AuditChange("permissionCode",
                        AuditEvent.ChangeType.SET,
                        AuditActionDefinition.DataClass.IDENTIFIER, null, "MODULE_PROSTHETICS_ACCESS")))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .occurredAt(occurredAt)
                .parentAuditId(parentAuditId)
                .source(AuditEvent.AuditSource.ADMIN_TOOL)
                .build());
    }

    @Test
    void search_combinedFilters_matchAndStableOrder() {
        UUID parentId = UUID.randomUUID();
        recordGrant(parentId, Instant.now().minusSeconds(120), null);
        recordGrant(UUID.randomUUID(), Instant.now().minusSeconds(60), parentId);

        JsonNode page = getJson(
                "/api/audit/events?module={module}&action={action}&outcome={outcome}&actorLogin={login}",
                getAdminToken(), "platform", "platform.rbac.permission.grant",
                "SUCCESS", syntheticLogin());

        assertThat(page.get("totalElements").asLong()).isGreaterThanOrEqualTo(2);
        List<String> actions = new java.util.ArrayList<>();
        page.get("content").forEach(node -> actions.add(node.get("action").asText()));
        assertThat(actions).allMatch("platform.rbac.permission.grant"::equals);
        // stable order: occurredAt desc, auditId desc
        List<String> occurred = new java.util.ArrayList<>();
        page.get("content").forEach(node -> occurred.add(node.get("occurredAt").asText()));
        assertThat(occurred).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    void detail_adminMasked_auditorFull_withIntegrityAndChildren() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        recordGrant(parentId, Instant.now().minusSeconds(120), null);
        recordGrant(childId, Instant.now().minusSeconds(60), parentId);

        JsonNode adminDetail = getJson("/api/audit/events/{id}", getAdminToken(), parentId);
        assertThat(adminDetail.get("auditId").asText()).isEqualTo(parentId.toString());
        assertThat(adminDetail.get("restrictedDetail").asBoolean()).isFalse();
        assertThat(adminDetail.get("integrityHash").asText()).isNotBlank();
        assertThat(adminDetail.get("integrityVerified").asBoolean()).isTrue();
        JsonNode adminChanges = adminDetail.get("changes");
        assertThat(adminChanges.size()).isEqualTo(1);
        assertThat(adminChanges.get(0).get("field").asText()).isEqualTo("permissionCode");
        assertThat(adminChanges.get(0).get("valuesRedacted").asBoolean()).isTrue();
        assertThat(adminChanges.get(0).get("newValue").isNull()).isTrue();
        JsonNode adminTargets = adminDetail.get("targets");
        assertThat(adminTargets.size()).isEqualTo(1);
        assertThat(adminTargets.get(0).get("relationType").asText()).isEqualTo("PRIMARY");
        assertThat(adminTargets.get(0).get("entityType").asText()).isEqualTo("User");
        JsonNode children = adminDetail.get("children");
        assertThat(children.size()).isEqualTo(1);
        assertThat(children.get(0).get("auditId").asText()).isEqualTo(childId.toString());

        String auditorToken = loginAs("auditor1", "doctor123");
        assertThat(auditorToken).isNotNull();
        JsonNode auditorDetail = getJson("/api/audit/events/{id}", auditorToken, parentId);
        assertThat(auditorDetail.get("restrictedDetail").asBoolean()).isTrue();
        JsonNode auditorChanges = auditorDetail.get("changes");
        assertThat(auditorChanges.get(0).get("valuesRedacted").asBoolean()).isFalse();
        assertThat(auditorChanges.get(0).get("newValue").asText())
                .isEqualTo("MODULE_PROSTHETICS_ACCESS");
    }

    @Test
    void objectHistory_returnsChronologyOldestFirst() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        recordGrant(parentId, Instant.now().minusSeconds(120), null);
        recordGrant(childId, Instant.now().minusSeconds(60), parentId);

        JsonNode history = getJson("/api/audit/entities/{type}/{id}",
                getAdminToken(), "User", syntheticUserId());

        assertThat(history.get("entityType").asText()).isEqualTo("User");
        assertThat(history.get("entityId").asText()).isEqualTo(syntheticUserId());
        assertThat(history.get("eventCount").asInt()).isGreaterThanOrEqualTo(2);
        List<String> occurred = new java.util.ArrayList<>();
        List<String> auditIds = new java.util.ArrayList<>();
        history.get("events").forEach(node -> {
            occurred.add(node.get("occurredAt").asText());
            auditIds.add(node.get("auditId").asText());
        });
        assertThat(occurred).isSorted();
        // this method's root precedes its child; sibling pairs from other
        // methods interleave by wall-clock, so only relative order is asserted
        assertThat(auditIds).contains(parentId.toString(), childId.toString());
        assertThat(auditIds.indexOf(parentId.toString())).isLessThan(auditIds.indexOf(childId.toString()));
    }

    @Test
    void consoleRead_isSelfAudited_andDeny_isRecorded() {
        restTemplate.exchange("/api/admin/users/16", HttpMethod.GET,
                authGet(getAdminToken()), String.class);

        JsonNode search = getJson(
                "/api/audit/events?module={module}&action={action}&targetType={type}&targetId={id}",
                getAdminToken(), "platform", "platform.user.view", "User", "16");
        assertThat(search.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);

        var denied = restTemplate.exchange("/api/audit/events", HttpMethod.GET,
                authGet(getNurseToken()), String.class);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        JsonNode denySearch = getJson("/api/audit/events?action={action}",
                getAdminToken(), "platform.auth.access.denied");
        assertThat(denySearch.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);
        boolean hasDeniedOutcome = false;
        for (JsonNode node : denySearch.get("content")) {
            if ("DENIED".equals(node.get("outcome").asText())) {
                hasDeniedOutcome = true;
            }
        }
        assertThat(hasDeniedOutcome).isTrue();
    }

    @Test
    void detail_unknownId_returns404() {
        var res = restTemplate.exchange("/api/audit/events/{id}", HttpMethod.GET,
                authGet(getAdminToken()), String.class, UUID.randomUUID());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
