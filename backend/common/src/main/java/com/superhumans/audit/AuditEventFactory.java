package com.superhumans.audit;

import org.springframework.stereotype.Component;

/** Attaches trusted backend request context to an already domain-classified event. */
@Component
public class AuditEventFactory {

    public AuditEvent attachRequestContext(AuditEvent event) {
        AuditRequestContext.Context context = AuditRequestContext.current();
        if (context == null) {
            return event;
        }
        AuditEvent.AuditHttpContext httpContext = event.httpContext() == null
                ? new AuditEvent.AuditHttpContext(context.httpMethod(), context.routeTemplate())
                : event.httpContext();
        Long durationMs = event.durationMs() == null
                ? Math.max(0L, (System.nanoTime() - context.startedAtNanos()) / 1_000_000L)
                : event.durationMs();
        return event.toBuilder()
                .requestId(event.requestId() == null ? context.requestId() : event.requestId())
                .userActionId(event.userActionId() == null ? context.userActionId() : event.userActionId())
                .correlationId(event.correlationId() == null ? context.correlationId() : event.correlationId())
                .httpContext(httpContext)
                .durationMs(durationMs)
                .build();
    }
}
