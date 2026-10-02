package com.superhumans.audit;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventFactoryTest {

    @Test
    void attachesRequestAndUiActionCorrelationWithoutChangingBusinessIdentity() {
        UUID requestId = UUID.randomUUID();
        UUID actionId = UUID.randomUUID();
        AuditEvent event = AuditEvent.builder()
                .actor(new AuditEvent.AuditActor(
                        AuditEvent.ActorType.USER, "11", "doctor1", null, Set.of("DOCTOR"), null, null))
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("episode")
                .action("icu.episode.create")
                .actionType(ActionType.CREATE)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.WEB_UI)
                .build();

        AuditRequestContext.Context context = new AuditRequestContext.Context(
                requestId, actionId, actionId, System.nanoTime(), "POST");
        context.setRouteTemplate("/api/episodes");
        AuditEvent enriched;
        try (AuditRequestContext.Scope ignored = AuditRequestContext.install(context)) {
            enriched = new AuditEventFactory().attachRequestContext(event);
        }

        assertThat(enriched.auditId()).isEqualTo(event.auditId());
        assertThat(enriched.requestId()).isEqualTo(requestId);
        assertThat(enriched.userActionId()).isEqualTo(actionId);
        assertThat(enriched.correlationId()).isEqualTo(actionId);
        assertThat(enriched.httpContext()).isEqualTo(new AuditEvent.AuditHttpContext("POST", "/api/episodes"));
        assertThat(enriched.durationMs()).isNotNegative();
        assertThat(AuditRequestContext.current()).isNull();
    }

    @Test
    void systemEventWithoutRequestContextRemainsUnchanged() {
        AuditEvent event = AuditEvent.builder()
                .actor(new AuditEvent.AuditActor(
                        AuditEvent.ActorType.SYSTEM, "daily-job", null, null, Set.of(), null, null))
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("clinical-day")
                .action("icu.clinical_day.auto_close")
                .actionType(ActionType.AUTO_CLOSE)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.SCHEDULED_JOB)
                .build();

        assertThat(new AuditEventFactory().attachRequestContext(event)).isSameAs(event);
    }
}
