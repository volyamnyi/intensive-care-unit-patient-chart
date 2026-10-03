package com.superhumans.service;

import com.superhumans.audit.AuditMetrics;
import com.superhumans.audit.LegacyAuditChecksum;
import com.superhumans.dto.LegacyBackfillReportResponse;
import com.superhumans.entity.core.AuditLegacyEvent;
import com.superhumans.repository.core.AuditLegacyEventRepository;
import com.superhumans.repository.core.LegacyAuditLogReader;
import com.superhumans.repository.core.LegacyAuditLogReader.LegacyAuditRow;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent backfill of {@code audit_logs} into {@code audit_legacy_events}
 * (F8, §H.4). Every source row — including {@code is_deleted} ones — becomes
 * exactly one frozen legacy row keyed by {@code (source_table, source_id)};
 * re-runs converge via the unique guard. Nothing is reconstructed: original
 * columns are copied verbatim, the outcome stays {@code UNKNOWN_LEGACY}.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LegacyAuditBackfillService {

    LegacyAuditLogReader legacyAuditLogReader;
    AuditLegacyEventRepository legacyEventRepository;
    AuditMetrics auditMetrics;

    @Transactional(transactionManager = "coreTransactionManager")
    public LegacyBackfillReportResponse backfill() {
        List<LegacyAuditRow> rows = legacyAuditLogReader.findAllOrdered();
        int inserted = 0;
        int skippedExisting = 0;
        int skippedInvalid = 0;
        int httpRequest = 0;
        int auditAction = 0;
        for (LegacyAuditRow row : rows) {
            if (row.getTimestamp() == null || row.getEntity() == null || row.getAction() == null) {
                skippedInvalid++;
                continue;
            }
            if (legacyEventRepository.existsBySourceTableAndSourceId(
                    LegacyAuditChecksum.SOURCE_TABLE, row.getId())) {
                skippedExisting++;
                continue;
            }
            try {
                legacyEventRepository.save(toLegacyEvent(row));
                inserted++;
            } catch (DataIntegrityViolationException duplicate) {
                skippedExisting++;
                continue;
            }
            if (LegacyAuditChecksum.KIND_HTTP_REQUEST.equals(LegacyAuditChecksum.kindOf(row.getAction()))) {
                httpRequest++;
            } else {
                auditAction++;
            }
        }
        auditMetrics.backfilled(inserted);
        return verify(rows, inserted, skippedExisting, skippedInvalid, httpRequest, auditAction);
    }

    @Transactional(transactionManager = "coreTransactionManager", readOnly = true)
    public LegacyBackfillReportResponse verifyOnly() {
        List<LegacyAuditRow> rows = legacyAuditLogReader.findAllOrdered();
        long invalid = rows.stream()
                .filter(row -> row.getTimestamp() == null || row.getEntity() == null || row.getAction() == null)
                .count();
        return verify(rows, 0, 0, (int) invalid, 0, 0);
    }

    /**
     * Verifies one scan snapshot: every scanned row has a faithful copy.
     * Rows committed after the snapshot are out of scope by design — the
     * next run converges on them (idempotency guard), so verification never
     * fails on a live, dual-writing system.
     */
    private LegacyBackfillReportResponse verify(List<LegacyAuditRow> rows, int inserted,
            int skippedExisting, int skippedInvalid, int httpRequest, int auditAction) {
        long sourceCount = legacyAuditLogReader.countAll();
        long backfilledCount = legacyEventRepository.count();
        List<String> mismatches = new ArrayList<>();
        List<UUID> missingCandidates = new ArrayList<>();
        for (LegacyAuditRow row : rows) {
            if (row.getTimestamp() == null || row.getEntity() == null || row.getAction() == null) {
                continue;
            }
            var stored = legacyEventRepository
                    .findBySourceTableAndSourceId(LegacyAuditChecksum.SOURCE_TABLE, row.getId());
            if (stored.isEmpty()) {
                missingCandidates.add(row.getId());
            } else if (!stored.get().getChecksum().equals(checksumOf(row))) {
                mismatches.add("checksum:" + row.getId());
            }
            if (mismatches.size() + missingCandidates.size() >= 100) {
                break;
            }
        }
        // Legacy writers keep running while the system is live (dual-write),
        // so a row committed between the scan and the check is re-checked once
        // instead of failing verification on a moving target.
        for (UUID candidate : missingCandidates) {
            if (legacyEventRepository
                    .findBySourceTableAndSourceId(LegacyAuditChecksum.SOURCE_TABLE, candidate).isEmpty()
                    && mismatches.size() < 100) {
                mismatches.add("missing:" + candidate);
            }
        }
        boolean verified = mismatches.isEmpty();
        return LegacyBackfillReportResponse.builder()
                .scanned(rows.size())
                .inserted(inserted)
                .skippedExisting(skippedExisting)
                .skippedInvalid(skippedInvalid)
                .httpRequestKind(httpRequest)
                .auditActionKind(auditAction)
                .legacySourceCount(sourceCount)
                .backfilledCount(backfilledCount)
                .checksumMismatches(List.copyOf(mismatches))
                .verified(verified)
                .build();
    }

    static AuditLegacyEvent toLegacyEvent(LegacyAuditRow row) {
        Instant occurredAt = LegacyAuditChecksum.occurredAtOf(row.getTimestamp());
        return AuditLegacyEvent.builder()
                .auditId(UUID.randomUUID())
                .sourceTable(LegacyAuditChecksum.SOURCE_TABLE)
                .sourceId(row.getId())
                .occurredAt(occurredAt)
                .legacyKind(LegacyAuditChecksum.kindOf(row.getAction()))
                .legacyEntity(row.getEntity())
                .legacyEntityId(row.getEntityId())
                .legacyAction(row.getAction())
                .legacyUserId(row.getUserId())
                .legacyUserRole(row.getUserRole())
                .legacyIpAddress(row.getIpAddress())
                .legacyOldValue(row.getOldValue())
                .legacyNewValue(row.getNewValue())
                .legacyDetails(row.getDetails())
                .legacyCorrelationId(row.getCorrelationId())
                .legacyIsDeleted(Boolean.TRUE.equals(row.getIsDeleted()))
                .schemaVersion(0)
                .contextCompleteness("LEGACY")
                .timestampPrecision("LEGACY_NAIVE")
                .outcome("UNKNOWN_LEGACY")
                .checksum(checksumOf(row))
                .retentionUntil(occurredAt.atZone(ZoneOffset.UTC).plusYears(2).toInstant())
                .build();
    }

    static String checksumOf(LegacyAuditRow row) {
        return LegacyAuditChecksum.sha256(LegacyAuditChecksum.canonical(
                row.getEntity(), row.getEntityId(), row.getAction(), row.getUserId(),
                row.getOldValue(), row.getNewValue(), row.getCorrelationId(), row.getDetails(),
                row.getIpAddress(), row.getUserRole(), row.getIsDeleted(), row.getTimestamp(),
                row.getId()));
    }
}
