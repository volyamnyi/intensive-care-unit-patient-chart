package com.superhumans.audit;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditEventTest {

    private static final AuditEvent.AuditActor USER = new AuditEvent.AuditActor(
            AuditEvent.ActorType.USER, "42", "doctor", "Doctor", Set.of("DOCTOR"), null, null);

    @Test
    void builderCreatesVersionedEventAndTwoYearProvisionalRetention() {
        Instant occurredAt = Instant.parse("2026-10-02T08:00:00Z");

        AuditEvent event = AuditEvent.builder()
                .occurredAt(occurredAt)
                .actor(USER)
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("episode")
                .action("icu.episode.create")
                .actionType(ActionType.CREATE)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.WEB_UI)
                .build();

        assertThat(event.auditId()).isNotNull();
        assertThat(event.schemaVersion()).isEqualTo(AuditEvent.CURRENT_SCHEMA_VERSION);
        assertThat(event.criticality()).isEqualTo(AuditActionDefinition.Criticality.HIGH);
        assertThat(event.retentionClass()).isEqualTo(AuditEvent.RetentionClass.STANDARD);
        assertThat(event.retentionUntil()).isEqualTo(occurredAt.atZone(java.time.ZoneOffset.UTC)
                .plusYears(2).toInstant());
        assertThat(event.changes()).isEmpty();
    }

    @Test
    void sensitivePatientReadGetsSensitiveAccessRetentionClass() {
        AuditEvent event = AuditEvent.builder()
                .actor(USER)
                .eventClass(EventClass.USER_ACTIVITY)
                .module("platform")
                .functionalArea("patient")
                .action("platform.patient.record.view")
                .actionType(ActionType.VIEW)
                .target(new AuditEvent.AuditTarget("Patient", "10001", null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.WEB_UI)
                .build();

        assertThat(event.retentionClass()).isEqualTo(AuditEvent.RetentionClass.SENSITIVE_ACCESS);
    }

    @Test
    void actionModuleAreaTypeAndClassMustMatchCatalog() {
        assertThatThrownBy(() -> AuditEvent.builder()
                .actor(USER)
                .eventClass(EventClass.BUSINESS)
                .module("medication")
                .functionalArea("episode")
                .action("icu.episode.create")
                .actionType(ActionType.CREATE)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.API)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match catalog");
    }

    @Test
    void actorMustMatchCatalogPolicy() {
        var system = new AuditEvent.AuditActor(
                AuditEvent.ActorType.SYSTEM, "clinical-day-job", null, null, Set.of(), null, null);

        assertThatThrownBy(() -> AuditEvent.builder()
                .actor(system)
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("episode")
                .action("icu.episode.create")
                .actionType(ActionType.CREATE)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.API)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actor does not match catalog policy");
    }

    @Test
    void clinicalOldNewValuesAreRejectedUntilRestrictedEncryptedStorageExists() {
        var sensitiveChange = new AuditEvent.AuditChange(
                "temperature", AuditEvent.ChangeType.SET, DataClass.CLINICAL, 36.5, 37.1);

        assertThatThrownBy(() -> eventWithChange(sensitiveChange))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("restricted encrypted storage");
    }

    @Test
    void secretFieldsAndUnapprovedMetadataAreRejected() {
        var secretChange = new AuditEvent.AuditChange(
                "passwordHash", AuditEvent.ChangeType.SET, DataClass.IDENTIFIER, null, "hash");
        assertThatThrownBy(() -> eventWithChange(secretChange))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Secret field");

        assertThatThrownBy(() -> AuditEvent.builder()
                .actor(USER)
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("episode")
                .action("icu.episode.create")
                .actionType(ActionType.CREATE)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.API)
                .metadata(Map.of("patientName", "Sensitive Name"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadata key is not allowlisted");
    }

    @Test
    void safeStatusDiffAndBoundedMetadataAreAccepted() {
        var statusChange = new AuditEvent.AuditChange(
                "status", AuditEvent.ChangeType.TRANSITION, DataClass.IDENTIFIER, "DRAFT", "ACTIVE");
        AuditEvent event = AuditEvent.builder()
                .actor(USER)
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("episode")
                .action("icu.episode.create")
                .actionType(ActionType.CREATE)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.API)
                .changes(java.util.List.of(statusChange))
                .metadata(Map.of("affectedRecords", 2))
                .build();

        assertThat(event.changes()).containsExactly(statusChange);
        assertThat(event.metadata()).containsEntry("affectedRecords", 2);
    }

    private AuditEvent eventWithChange(AuditEvent.AuditChange change) {
        return AuditEvent.builder()
                .actor(USER)
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("episode")
                .action("icu.episode.create")
                .actionType(ActionType.CREATE)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.API)
                .changes(java.util.List.of(change))
                .build();
    }
}
