package com.superhumans.audit;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.service.CoreAuditEventWriter;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditEventRecorderTest {

    @Mock
    private AuditEventFactory eventFactory;

    @Mock
    private CoreAuditEventWriter writer;

    @Mock
    private AuditMetrics auditMetrics;

    @InjectMocks
    private AuditEventRecorder recorder;

    private static AuditEvent event() {
        return AuditEvent.builder()
                .actor(new AuditEvent.AuditActor(
                        AuditEvent.ActorType.USER, "11", "doctor1", null, Set.of("DOCTOR"), null, null))
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("session")
                .action("platform.auth.session.logout")
                .actionType(ActionType.LOGOUT)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.API)
                .build();
    }

    @Test
    void record_enrichesAndAppends() {
        AuditEvent event = event();
        org.mockito.Mockito.when(eventFactory.attachRequestContext(event)).thenReturn(event);

        recorder.record(event);

        verify(writer).append(event);
    }

    @Test
    void record_writerFailureIsSwallowed() {
        AuditEvent event = event();
        org.mockito.Mockito.when(eventFactory.attachRequestContext(event)).thenReturn(event);
        doThrow(new RuntimeException("store down")).when(writer).append(any());

        assertThatCode(() -> recorder.record(event)).doesNotThrowAnyException();
    }

    @Test
    void record_factoryFailureIsSwallowed() {
        AuditEvent event = event();
        doThrow(new RuntimeException("no context")).when(eventFactory).attachRequestContext(event);

        assertThatCode(() -> recorder.record(event)).doesNotThrowAnyException();
    }
}
