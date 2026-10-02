package com.superhumans.audit;

import java.security.MessageDigest;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Canonical JSON serialization and payload digest used by module outboxes. */
@Component
@RequiredArgsConstructor
public class AuditEventSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(AuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Audit event could not be serialized", exception);
        }
    }

    public String sha256(AuditEvent event) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(event));
            return HexFormat.of().formatHex(digest);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Audit event could not be serialized", exception);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public AuditEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, AuditEvent.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Audit event payload is invalid", exception);
        }
    }
}
