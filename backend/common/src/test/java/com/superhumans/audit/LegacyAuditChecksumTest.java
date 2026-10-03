package com.superhumans.audit;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyAuditChecksumTest {

    @Test
    void kindOf_classifiesApiTransportVsManualActions() {
        assertThat(LegacyAuditChecksum.kindOf("API_PATIENT_SEARCH"))
                .isEqualTo(LegacyAuditChecksum.KIND_HTTP_REQUEST);
        assertThat(LegacyAuditChecksum.kindOf("CREATE")).isEqualTo(LegacyAuditChecksum.KIND_AUDIT_ACTION);
        assertThat(LegacyAuditChecksum.kindOf(null)).isEqualTo(LegacyAuditChecksum.KIND_AUDIT_ACTION);
    }

    @Test
    void checksum_isStableAndVerbatim() {
        UUID sourceId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 15, 10, 30, 0);
        String first = LegacyAuditChecksum.sha256(LegacyAuditChecksum.canonical(
                "Episode", entityId, "CREATE", 11L, null, "Updated episode fields",
                null, null, "127.0.0.1", "DOCTOR", false, timestamp, sourceId));
        String second = LegacyAuditChecksum.sha256(LegacyAuditChecksum.canonical(
                "Episode", entityId, "CREATE", 11L, null, "Updated episode fields",
                null, null, "127.0.0.1", "DOCTOR", false, timestamp, sourceId));
        assertThat(first).isEqualTo(second).hasSize(64);

        String changed = LegacyAuditChecksum.sha256(LegacyAuditChecksum.canonical(
                "Episode", entityId, "CREATE", 11L, null, "Different text",
                null, null, "127.0.0.1", "DOCTOR", false, timestamp, sourceId));
        assertThat(changed).isNotEqualTo(first);
    }

    @Test
    void checksum_toleratesNullsAndUnicode() {
        String checksum = LegacyAuditChecksum.sha256(LegacyAuditChecksum.canonical(
                "Пацієнт", null, null, null, null, null, null, "кирилиця ✓", null, null, null, null,
                UUID.randomUUID()));
        assertThat(checksum).hasSize(64);
    }

    @Test
    void occurredAtOf_assumesUtc() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 15, 10, 30, 0);
        assertThat(LegacyAuditChecksum.occurredAtOf(timestamp).toString())
                .isEqualTo("2026-01-15T10:30:00Z");
        assertThat(LegacyAuditChecksum.occurredAtOf(null)).isNull();
    }
}
