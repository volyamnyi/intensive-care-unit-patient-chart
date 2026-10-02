package com.superhumans.audit;

import com.superhumans.service.CoreAuditEventWriter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fail-safe entry point for security and platform event recording.
 * A recording failure is logged and swallowed so auditing can never break
 * authentication, authorization or any business operation (policy §B4).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditEventRecorder {

    AuditEventFactory eventFactory;
    CoreAuditEventWriter writer;

    public void record(AuditEvent event) {
        try {
            AuditEvent enriched = eventFactory.attachRequestContext(event);
            writer.append(enriched);
        } catch (RuntimeException exception) {
            log.warn("Audit event recording failed action={} errorType={}",
                    event == null ? null : event.action(), exception.getClass().getSimpleName());
        }
    }
}
