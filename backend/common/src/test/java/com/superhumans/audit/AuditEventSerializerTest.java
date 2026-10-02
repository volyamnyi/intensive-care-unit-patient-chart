package com.superhumans.audit;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventSerializerTest {

    @Test
    void serializationAndDigestAreDeterministicForEquivalentEvents() {
        var actor = new AuditEvent.AuditActor(
                AuditEvent.ActorType.USER, "42", "doctor", null, Set.of("NURSE", "DOCTOR"), null, null);
        AuditEvent first = event(actor, Map.of("attempt", 2, "deliveryKind", "SINGLE"));
        AuditEvent second = event(actor, Map.of("deliveryKind", "SINGLE", "attempt", 2));
        var serializer = new AuditEventSerializer(new ObjectMapper());

        assertThat(serializer.serialize(first)).isEqualTo(serializer.serialize(second));
        assertThat(serializer.sha256(first)).isEqualTo(serializer.sha256(second)).hasSize(64);
    }

    private AuditEvent event(AuditEvent.AuditActor actor, Map<String, Object> metadata) {
        return AuditEvent.builder()
                .auditId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .occurredAt(Instant.parse("2026-10-02T08:00:00Z"))
                .actor(actor)
                .eventClass(EventClass.BUSINESS)
                .module("prosthetics")
                .functionalArea("notification")
                .action("prosthetics.notification.queue")
                .actionType(ActionType.QUEUE)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.WEB_UI)
                .metadata(metadata)
                .build();
    }
}
