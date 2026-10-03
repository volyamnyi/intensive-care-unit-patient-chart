package com.superhumans.audit;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Canonical form of one legacy {@code audit_logs} row (F8 backfill, §H.4).
 * The checksum covers the verbatim source columns in a fixed order — the
 * same canonicalizer runs at insert, at verify, and at detail-integrity
 * time, so any drift is detectable. No business facts are inferred.
 */
public final class LegacyAuditChecksum {

    public static final String SOURCE_TABLE = "audit_logs";
    public static final String KIND_HTTP_REQUEST = "legacy.http.request";
    public static final String KIND_AUDIT_ACTION = "legacy.audit.action";

    private LegacyAuditChecksum() {
    }

    /** {@code API_*} rows are transport traces, old manual rows are audit actions (§H.4). */
    public static String kindOf(String action) {
        return action != null && action.startsWith("API_") ? KIND_HTTP_REQUEST : KIND_AUDIT_ACTION;
    }

    /** Naive source timestamp kept as a UTC-assumed instant (offset unknown, flagged LEGACY_NAIVE). */
    public static java.time.Instant occurredAtOf(LocalDateTime timestamp) {
        return timestamp == null ? null : timestamp.toInstant(ZoneOffset.UTC);
    }

    public static String canonical(String entity, UUID entityId, String action, Long userId,
            String oldValue, String newValue, String correlationId, String details,
            String ipAddress, String userRole, Boolean isDeleted, LocalDateTime timestamp,
            UUID sourceId) {
        return join(entity) + "\0"
                + join(entityId == null ? null : entityId.toString()) + "\0"
                + join(action) + "\0"
                + join(userId == null ? null : userId.toString()) + "\0"
                + join(oldValue) + "\0"
                + join(newValue) + "\0"
                + join(correlationId) + "\0"
                + join(details) + "\0"
                + join(ipAddress) + "\0"
                + join(userRole) + "\0"
                + join(isDeleted == null ? null : isDeleted.toString()) + "\0"
                + join(timestamp == null ? null : timestamp.toString()) + "\0"
                + join(sourceId == null ? null : sourceId.toString());
    }

    public static String sha256(String canonical) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String join(String value) {
        return value == null ? "" : value;
    }
}
