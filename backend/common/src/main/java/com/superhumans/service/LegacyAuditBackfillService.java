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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 *
 * <p>The scan is keyset-paged ({@code id > lastId}) and both the existence
 * check and the verification pass run as one batched query per page, so the
 * statement count stays proportional to the page count instead of the row count
 * (#429). Verification therefore compares every valid scanned row, not a
 * truncated sample.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LegacyAuditBackfillService {

    /** Rows read per keyset page; bounds heap use on large legacy tables. */
    static final int PAGE_SIZE = 1000;

    LegacyAuditLogReader legacyAuditLogReader;
    AuditLegacyEventRepository legacyEventRepository;
    AuditMetrics auditMetrics;

    @Transactional(transactionManager = "coreTransactionManager")
    public LegacyBackfillReportResponse backfill() {
        BackfillAccumulator totals = new BackfillAccumulator();
        UUID cursor = null;
        while (true) {
            List<LegacyAuditRow> page = legacyAuditLogReader.findOrderedPage(cursor, PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            processPage(page, totals, true);
            totals.scanned += page.size();
            cursor = page.get(page.size() - 1).getId();
            if (page.size() < PAGE_SIZE) {
                break;
            }
        }
        auditMetrics.backfilled(totals.inserted);
        return report(totals);
    }

    @Transactional(transactionManager = "coreTransactionManager", readOnly = true)
    /**
     * No read-only verification endpoint is exposed (#397 item 7): re-running the
     * idempotent backfill IS the verification (see runbook F.1 step 3). This method
     * stays as the internal verify primitive.
     */
    public LegacyBackfillReportResponse verifyOnly() {
        BackfillAccumulator totals = new BackfillAccumulator();
        UUID cursor = null;
        while (true) {
            List<LegacyAuditRow> page = legacyAuditLogReader.findOrderedPage(cursor, PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            totals.scanned += page.size();
            totals.invalidRows += invalidCount(page);
            verifyPage(page, totals);
            cursor = page.get(page.size() - 1).getId();
            if (page.size() < PAGE_SIZE) {
                break;
            }
        }
        return report(totals);
    }

    private void processPage(List<LegacyAuditRow> page, BackfillAccumulator totals, boolean insert) {
        List<LegacyAuditRow> valid = page.stream().filter(this::isValid).toList();
        totals.invalidRows += page.size() - valid.size();
        if (valid.isEmpty()) {
            return;
        }
        Set<UUID> ids = new HashSet<>();
        for (LegacyAuditRow row : valid) {
            ids.add(row.getId());
        }
        Map<UUID, AuditLegacyEvent> storedBeforeInsert = storedBySourceId(ids);

        List<AuditLegacyEvent> toInsert = valid.stream()
                .filter(row -> !storedBeforeInsert.containsKey(row.getId()))
                .map(LegacyAuditBackfillService::toLegacyEvent)
                .toList();
        if (insert && !toInsert.isEmpty()) {
            try {
                legacyEventRepository.saveAll(toInsert);
            } catch (DataIntegrityViolationException duplicate) {
                // Unique backstop: a concurrent run won the row. Nothing else is
                // recoverable inside this transaction, matching the pre-existing
                // per-row contract (runbook F.1 requires a re-run to converge).
                throw duplicate;
            }
            for (AuditLegacyEvent event : toInsert) {
                totals.inserted++;
                if (LegacyAuditChecksum.KIND_HTTP_REQUEST.equals(event.getLegacyKind())) {
                    totals.httpRequest++;
                } else {
                    totals.auditAction++;
                }
            }
        }
        Map<UUID, AuditLegacyEvent> stored = insert && !toInsert.isEmpty()
                ? storedBySourceId(ids)
                : storedBeforeInsert;
        totals.existing += valid.size() - toInsert.size();
        for (LegacyAuditRow row : valid) {
            AuditLegacyEvent event = stored.get(row.getId());
            if (event == null) {
                totals.mismatches.add("missing:" + row.getId());
            } else if (!event.getChecksum().equals(checksumOf(row))) {
                totals.mismatches.add("checksum:" + row.getId());
            }
        }
    }

    private void verifyPage(List<LegacyAuditRow> page, BackfillAccumulator totals) {
        List<LegacyAuditRow> valid = page.stream().filter(this::isValid).toList();
        if (valid.isEmpty()) {
            return;
        }
        Map<UUID, AuditLegacyEvent> stored = storedBySourceId(
                valid.stream().map(LegacyAuditRow::getId).collect(java.util.stream.Collectors.toSet()));
        for (LegacyAuditRow row : valid) {
            AuditLegacyEvent event = stored.get(row.getId());
            if (event == null) {
                totals.mismatches.add("missing:" + row.getId());
            } else if (!event.getChecksum().equals(checksumOf(row))) {
                totals.mismatches.add("checksum:" + row.getId());
            }
        }
    }

    private Map<UUID, AuditLegacyEvent> storedBySourceId(Set<UUID> ids) {
        Map<UUID, AuditLegacyEvent> bySourceId = new HashMap<>();
        for (AuditLegacyEvent event
                : legacyEventRepository.findBySourceTableAndSourceIds(
                        LegacyAuditChecksum.SOURCE_TABLE, ids)) {
            bySourceId.put(event.getSourceId(), event);
        }
        return bySourceId;
    }

    private static long invalidCount(List<LegacyAuditRow> page) {
        return page.stream().filter(row -> !isValidRow(row)).count();
    }

    private boolean isValid(LegacyAuditRow row) {
        return isValidRow(row);
    }

    private static boolean isValidRow(LegacyAuditRow row) {
        return row.getTimestamp() != null && row.getEntity() != null && row.getAction() != null;
    }

    private LegacyBackfillReportResponse report(BackfillAccumulator totals) {
        return LegacyBackfillReportResponse.builder()
                .scanned(totals.scanned)
                .inserted(totals.inserted)
                .skippedExisting(totals.existing)
                .skippedInvalid(totals.invalidRows)
                .httpRequestKind(totals.httpRequest)
                .auditActionKind(totals.auditAction)
                .legacySourceCount(legacyAuditLogReader.countAll())
                .backfilledCount(legacyEventRepository.count())
                .checksumMismatches(List.copyOf(totals.mismatches))
                .verified(totals.mismatches.isEmpty())
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

    /** Running totals across keyset pages. */
    private static final class BackfillAccumulator {
        private long scanned;
        private int inserted;
        private int existing;
        private long invalidRows;
        private int httpRequest;
        private int auditAction;
        private final List<String> mismatches = new ArrayList<>();
    }
}