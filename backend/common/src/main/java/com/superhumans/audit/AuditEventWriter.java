package com.superhumans.audit;

/**
 * Module-facing append boundary. Feature implementations append to the module-local durable
 * outbox in the caller's business transaction; the platform implementation appends directly
 * to the core event store in the core transaction. Implementations never write remotely.
 */
@FunctionalInterface
public interface AuditEventWriter {

    void append(AuditEvent event);
}
