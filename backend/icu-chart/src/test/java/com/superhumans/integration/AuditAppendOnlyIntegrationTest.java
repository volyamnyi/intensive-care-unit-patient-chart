package com.superhumans.integration;

import com.superhumans.audit.AuditActionDefinition;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventSerializer;
import com.superhumans.entity.core.AuditEventEntity;
import com.superhumans.entity.core.AuditEventTargetEntity;
import com.superhumans.entity.core.AuditEventTargetId;
import com.superhumans.entity.core.AuditLegacyEvent;
import com.superhumans.repository.core.AuditEventRepository;
import com.superhumans.repository.core.AuditEventTargetRepository;
import com.superhumans.repository.core.AuditLegacyEventRepository;
import com.superhumans.service.AuditEventPersistenceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional("coreTransactionManager")
class AuditAppendOnlyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    AuditEventRepository eventRepository;

    @Autowired
    AuditEventTargetRepository targetRepository;

    @Autowired
    AuditLegacyEventRepository legacyEventRepository;

    @Autowired
    AuditEventPersistenceService persistenceService;

    @Autowired
    AuditEventSerializer serializer;

    @PersistenceContext
    EntityManager entityManager;

    private UUID persistCanonicalEvent() {        AuditEvent event = AuditEvent.builder()
                .actor(new AuditEvent.AuditActor(AuditEvent.ActorType.UNKNOWN,
                        null, null, null, Set.of(), null, null))
                .eventClass(AuditActionDefinition.EventClass.SECURITY)
                .module("platform")
                .functionalArea("token")
                .action("platform.auth.token.rejected")
                .actionType(AuditActionDefinition.ActionType.ACCESS_DENIED)
                .outcome(AuditEvent.AuditOutcome.FAILURE)
                .source(AuditEvent.AuditSource.API)
                .build();
        persistenceService.persist(event, serializer.sha256(event));
        return event.auditId();
    }

    private static void mutate(Object entity, String field, Object value) throws Exception {
        Field declared = entity.getClass().getDeclaredField(field);
        declared.setAccessible(true);
        declared.set(entity, value);
    }

    @Test
    void canonicalEvent_cannotBeUpdatedOrDeleted() throws Exception {
        UUID auditId = persistCanonicalEvent();
        AuditEventEntity stored = eventRepository.findById(auditId).orElseThrow();
        String payloadBefore = stored.getEventPayload();
        String hashBefore = stored.getIntegrityHash();

        mutate(stored, "outcome", "SUCCESS");
        try {
            eventRepository.saveAndFlush(stored);
        } catch (RuntimeException rejected) {
            // Immutable rejection is one acceptable enforcement path.
        }
        entityManager.clear();

        AuditEventEntity reread = eventRepository.findById(auditId).orElseThrow();
        assertThat(reread.getOutcome()).isEqualTo("FAILURE");
        // PostgreSQL jsonb normalizes key order/whitespace on write, so the
        // payload is compared semantically, not as a raw string.
        assertThat(new tools.jackson.databind.ObjectMapper().readTree(reread.getEventPayload()))
                .isEqualTo(new tools.jackson.databind.ObjectMapper().readTree(payloadBefore));
        assertThat(reread.getIntegrityHash()).isEqualTo(hashBefore);

        assertThatThrownBy(() -> eventRepository.deleteById(auditId))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void eventTarget_cannotBeUpdatedOrDeleted() throws Exception {
        UUID parentId = persistCanonicalEvent();
        AuditEventTargetId targetId = new AuditEventTargetId(
                parentId, "PRIMARY", "Episode", UUID.randomUUID().toString());
        targetRepository.save(AuditEventTargetEntity.builder()
                .auditId(targetId.getAuditId())
                .relationType(targetId.getRelationType())
                .entityType(targetId.getEntityType())
                .entityId(targetId.getEntityId())
                .occurredAt(Instant.now())
                .build());
        entityManager.flush();
        entityManager.clear();

        AuditEventTargetEntity stored = targetRepository.findById(targetId).orElseThrow();
        mutate(stored, "businessKey", "tampered");
        try {
            targetRepository.saveAndFlush(stored);
        } catch (RuntimeException rejected) {
            // Immutable rejection is one acceptable enforcement path.
        }
        entityManager.clear();

        assertThat(targetRepository.findById(targetId).orElseThrow().getBusinessKey()).isNull();
        assertThatThrownBy(() -> targetRepository.deleteById(targetId))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void legacyEvent_cannotBeUpdatedOrDeleted() throws Exception {
        UUID auditId = UUID.randomUUID();
        legacyEventRepository.save(AuditLegacyEvent.builder()
                .auditId(auditId)
                .sourceTable("audit_logs")
                .sourceId(UUID.randomUUID())
                .occurredAt(Instant.now())
                .legacyKind("legacy.audit.action")
                .legacyEntity("Episode")
                .legacyAction("CREATE")
                .legacyIsDeleted(false)
                .schemaVersion(0)
                .contextCompleteness("LEGACY")
                .timestampPrecision("LEGACY_NAIVE")
                .outcome("UNKNOWN_LEGACY")
                .checksum("abc")
                .retentionUntil(Instant.now().plusSeconds(3600))
                .build());
        entityManager.flush();
        entityManager.clear();

        AuditLegacyEvent stored = legacyEventRepository.findById(auditId).orElseThrow();
        mutate(stored, "outcome", "SUCCESS");
        try {
            legacyEventRepository.saveAndFlush(stored);
        } catch (RuntimeException rejected) {
            // Immutable rejection is one acceptable enforcement path.
        }
        entityManager.clear();

        assertThat(legacyEventRepository.findById(auditId).orElseThrow().getOutcome())
                .isEqualTo("UNKNOWN_LEGACY");
        assertThatThrownBy(() -> legacyEventRepository.deleteById(auditId))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
