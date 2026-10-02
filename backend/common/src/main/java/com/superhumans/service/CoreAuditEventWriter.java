package com.superhumans.service;

import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventSerializer;
import com.superhumans.audit.AuditEventWriter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

/** Direct core-database writer for platform and security events. */
@Component("platformAuditEventWriter")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CoreAuditEventWriter implements AuditEventWriter {

    AuditEventPersistenceService persistenceService;
    AuditEventSerializer serializer;

    @Override
    public void append(AuditEvent event) {
        if (!"platform".equals(event.module())) {
            throw new IllegalArgumentException("Core event writer only accepts platform events");
        }
        persistenceService.persist(event, serializer.sha256(event));
    }
}
