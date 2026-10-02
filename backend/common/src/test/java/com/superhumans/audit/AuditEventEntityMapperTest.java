package com.superhumans.audit;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.entity.core.AuditEventEntity;
import com.superhumans.mapper.AuditEventEntityMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventEntityMapperTest {

    @Test
    void mapsSearchProjectionAndKeepsCanonicalEventPayload() {
        AuditEvent event = AuditEvent.builder()
                .actor(new AuditEvent.AuditActor(
                        AuditEvent.ActorType.USER, "42", "doctor", "Doctor", Set.of("DOCTOR"), null, null))
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("episode")
                .action("icu.episode.create")
                .actionType(ActionType.CREATE)
                .target(new AuditEvent.AuditTarget("Episode", "episode-1", "EPI-1"))
                .relatedEntities(List.of(new AuditEvent.AuditTarget("ClinicalDay", "day-1", null)))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.WEB_UI)
                .changes(List.of(new AuditEvent.AuditChange(
                        "status", AuditEvent.ChangeType.SET, DataClass.IDENTIFIER, null, "ACTIVE")))
                .build();

        AuditEventEntity entity = new AuditEventEntityMapper(new ObjectMapper()).toEntity(event, "a".repeat(64));

        assertThat(entity.getAuditId()).isEqualTo(event.auditId());
        assertThat(entity.getModule()).isEqualTo("icu");
        assertThat(entity.getAction()).isEqualTo("icu.episode.create");
        assertThat(entity.getCriticality()).isEqualTo("HIGH");
        assertThat(entity.getActorId()).isEqualTo("42");
        assertThat(entity.getTargetId()).isEqualTo("episode-1");
        assertThat(entity.getBusinessKey()).isEqualTo("EPI-1");
        assertThat(entity.getIntegrityHash()).hasSize(64);
        JsonNode payload = new ObjectMapper().readTree(entity.getEventPayload());
        assertThat(payload.get("auditId").asText()).isEqualTo(event.auditId().toString());
        assertThat(payload.get("changes").get(0).get("field").asText()).isEqualTo("status");

        var targets = new AuditEventEntityMapper(new ObjectMapper()).toTargetEntities(event);
        assertThat(targets).hasSize(2);
        assertThat(targets).extracting("relationType").containsExactly("PRIMARY", "RELATED");
    }
}
