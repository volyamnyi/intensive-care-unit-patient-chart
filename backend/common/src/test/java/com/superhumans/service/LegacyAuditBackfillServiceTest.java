package com.superhumans.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superhumans.audit.AuditMetrics;
import com.superhumans.audit.LegacyAuditChecksum;
import com.superhumans.entity.core.AuditLegacyEvent;
import com.superhumans.repository.core.AuditLegacyEventRepository;
import com.superhumans.repository.core.LegacyAuditLogReader;
import com.superhumans.repository.core.LegacyAuditLogReader.LegacyAuditRow;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

/**
 * #429 item 4: the backfill scans {@code audit_logs} in keyset pages and does
 * its existence/checksum work in one batched query per page, so the statement
 * count follows the page count instead of the row count.
 */
class LegacyAuditBackfillServiceTest {

    private final LegacyAuditLogReader reader = mock(LegacyAuditLogReader.class);
    private final AuditLegacyEventRepository repository = mock(AuditLegacyEventRepository.class);
    private final AuditMetrics metrics = mock(AuditMetrics.class);
    private final LegacyAuditBackfillService service =
            new LegacyAuditBackfillService(reader, repository, metrics);

    /** Minimal projection fake; {@code valid=false} marks an unusable row. */
    private record Row(UUID id, UUID entityId, String action, boolean valid)
            implements LegacyAuditRow {
        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return valid ? LocalDateTime.of(2026, 1, 1, 8, 0) : null;
        }

        @Override
        public Long getUserId() {
            return 11L;
        }

        @Override
        public String getEntity() {
            return valid ? "Entity" : null;
        }

        @Override
        public UUID getEntityId() {
            return entityId;
        }

        @Override
        public String getAction() {
            return action;
        }

        @Override
        public String getOldValue() {
            return null;
        }

        @Override
        public String getNewValue() {
            return null;
        }

        @Override
        public String getCorrelationId() {
            return null;
        }

        @Override
        public String getDetails() {
            return null;
        }

        @Override
        public String getIpAddress() {
            return null;
        }

        @Override
        public String getUserRole() {
            return "DOCTOR";
        }

        @Override
        public Boolean getIsDeleted() {
            return Boolean.FALSE;
        }
    }

    private static Row validRow(String action) {
        return new Row(UUID.randomUUID(), UUID.randomUUID(), action, true);
    }

    private static Row invalidRow() {
        return new Row(UUID.randomUUID(), UUID.randomUUID(), "CREATE", false);
    }

    private static List<LegacyAuditRow> page(int size) {
        List<LegacyAuditRow> rows = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            rows.add(validRow("CREATE"));
        }
        return rows;
    }

    private Map<UUID, String> checksums(List<? extends LegacyAuditRow>... pages) {
        Map<UUID, String> map = new HashMap<>();
        for (List<? extends LegacyAuditRow> page : pages) {
            for (LegacyAuditRow row : page) {
                map.put(row.getId(), LegacyAuditBackfillService.checksumOf(row));
            }
        }
        return map;
    }

    private final Set<UUID> storedIds = new LinkedHashSet<>();

    /** Every listed row is already copied; inserts extend {@code storedIds}. */
    private void stubAlreadyBackfilled(List<? extends LegacyAuditRow>... pages) {
        stubStoredState(checksums(pages), idsOf(pages));
    }

    /** Nothing copied yet: the first existence lookup is empty, then inserts. */
    private void stubBackfilledAfterInsert(List<? extends LegacyAuditRow>... pages) {
        stubStoredState(checksums(pages), Set.of());
    }

    @SafeVarargs
    private static Set<UUID> idsOf(List<? extends LegacyAuditRow>... pages) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (List<? extends LegacyAuditRow> page : pages) {
            for (LegacyAuditRow row : page) {
                ids.add(row.getId());
            }
        }
        return ids;
    }

    /** Miniature in-memory store behind the batched repository lookup. */
    private void stubStoredState(Map<UUID, String> expectedChecksums, Set<UUID> preExisting) {
        storedIds.addAll(preExisting);
        when(repository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<AuditLegacyEvent> events = invocation.getArgument(0);
            // Null arrives while a later test stub is being installed.
            if (events == null) {
                return List.of();
            }
            for (AuditLegacyEvent event : events) {
                storedIds.add(event.getSourceId());
            }
            return events;
        });
        when(repository.findBySourceTableAndSourceIds(anyString(), any()))
                .thenAnswer(invocation -> {
                    Collection<UUID> ids = invocation.getArgument(1);
                    List<AuditLegacyEvent> stored = new ArrayList<>();
                    for (UUID id : ids) {
                        if (!storedIds.contains(id)) {
                            continue;
                        }
                        var entity = mock(AuditLegacyEvent.class);
                        when(entity.getSourceId()).thenReturn(id);
                        when(entity.getChecksum()).thenReturn(expectedChecksums.get(id));
                        stored.add(entity);
                    }
                    return stored;
                });
    }

    @Test
    void backfill_walksPagesWithCursorAndBatchesPerPage() {
        int pageSize = LegacyAuditBackfillService.PAGE_SIZE;
        List<LegacyAuditRow> firstPage = page(pageSize);
        Row tail = validRow("UPDATE");

        when(reader.findOrderedPage(any(), anyInt()))
                .thenReturn(firstPage)
                .thenReturn(List.of(tail))
                .thenReturn(List.of());
        stubBackfilledAfterInsert(firstPage, List.of(tail));
        when(repository.count()).thenReturn((long) pageSize + 1);
        when(reader.countAll()).thenReturn((long) pageSize + 1);

        var report = service.backfill();

        assertThat(report.getScanned()).isEqualTo(pageSize + 1L);
        assertThat(report.getInserted()).isEqualTo(pageSize + 1);
        assertThat(report.getChecksumMismatches()).isEmpty();
        assertThat(report.isVerified()).isTrue();
        // Each inserting page costs exactly two batched lookups (existence +
        // post-insert verification) regardless of its row count.
        verify(repository, times(4)).findBySourceTableAndSourceIds(anyString(), any());
        verify(repository, times(2)).saveAll(any());
        verify(reader).findOrderedPage(null, pageSize);
        verify(reader).findOrderedPage(firstPage.get(firstPage.size() - 1).getId(), pageSize);
        verify(metrics).backfilled(pageSize + 1);
    }

    @Test
    void backfill_countsAlreadyCopiedRowsAndInsertsNothing() {
        Row copied = validRow("CREATE");
        when(reader.findOrderedPage(any(), anyInt()))
                .thenReturn(List.of(copied))
                .thenReturn(List.of());
        stubAlreadyBackfilled(List.of(copied));
        when(repository.count()).thenReturn(1L);
        when(reader.countAll()).thenReturn(1L);

        var report = service.backfill();

        assertThat(report.getSkippedExisting()).isEqualTo(1);
        assertThat(report.getInserted()).isZero();
        assertThat(report.isVerified()).isTrue();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void backfill_reportsChecksumDriftAsUnverified() {
        Row drifted = validRow("CREATE");
        when(reader.findOrderedPage(any(), anyInt()))
                .thenReturn(List.of(drifted))
                .thenReturn(List.of());
        when(repository.findBySourceTableAndSourceIds(anyString(), any()))
                .thenAnswer(invocation -> {
                    Collection<UUID> ids = invocation.getArgument(1);
                    List<AuditLegacyEvent> stored = new ArrayList<>();
                    for (UUID id : ids) {
                        var entity = mock(AuditLegacyEvent.class);
                        when(entity.getSourceId()).thenReturn(id);
                        when(entity.getChecksum()).thenReturn("different-digest");
                        stored.add(entity);
                    }
                    return stored;
                });
        when(repository.count()).thenReturn(1L);
        when(reader.countAll()).thenReturn(1L);

        var report = service.backfill();

        assertThat(report.getChecksumMismatches()).hasSize(1);
        assertThat(report.isVerified()).isFalse();
    }

    @Test
    void backfill_reportsMissingCopyAsUnverified() {
        when(reader.findOrderedPage(any(), anyInt()))
                .thenReturn(List.of(validRow("CREATE")))
                .thenReturn(List.of());
        when(repository.findBySourceTableAndSourceIds(anyString(), any())).thenReturn(List.of());
        when(repository.saveAll(any())).thenReturn(List.of());
        when(repository.count()).thenReturn(0L);
        when(reader.countAll()).thenReturn(1L);

        var report = service.backfill();

        assertThat(report.getChecksumMismatches()).hasSize(1);
        assertThat(report.isVerified()).isFalse();
    }

    @Test
    void backfill_countsInvalidRowsWithoutQueryingThem() {
        when(reader.findOrderedPage(any(), anyInt()))
                .thenReturn(List.of(invalidRow()))
                .thenReturn(List.of());
        when(repository.count()).thenReturn(0L);
        when(reader.countAll()).thenReturn(1L);

        var report = service.backfill();

        assertThat(report.getSkippedInvalid()).isEqualTo(1L);
        assertThat(report.isVerified()).isTrue();
        verify(repository, never()).findBySourceTableAndSourceIds(anyString(), any());
        verify(repository, never()).saveAll(any());
    }

    @Test
    void verifyOnly_neverInserts() {
        Row row = validRow("CREATE");
        when(reader.findOrderedPage(any(), anyInt()))
                .thenReturn(List.of(row))
                .thenReturn(List.of());
        stubAlreadyBackfilled(List.of(row));
        when(repository.count()).thenReturn(1L);
        when(reader.countAll()).thenReturn(1L);

        var report = service.verifyOnly();

        assertThat(report.getInserted()).isZero();
        assertThat(report.getScanned()).isEqualTo(1L);
        assertThat(report.isVerified()).isTrue();
        verify(repository, never()).saveAll(any());
        verify(metrics, never()).backfilled(anyInt());
    }

    @Test
    void pagedScan_advancesCursorOnlyWhenAPageIsFull() {
        int pageSize = LegacyAuditBackfillService.PAGE_SIZE;
        List<LegacyAuditRow> firstPage = page(pageSize);
        Row second = validRow("CREATE");
        when(reader.findOrderedPage(any(), anyInt()))
                .thenReturn(firstPage)
                .thenReturn(List.of(second))
                .thenReturn(List.of());
        stubBackfilledAfterInsert(firstPage, List.of(second));
        when(repository.count()).thenReturn((long) pageSize + 1);
        when(reader.countAll()).thenReturn((long) pageSize + 1);

        service.backfill();

        // Cursor advances from a full page, so no row between pages is skipped.
        verify(reader).findOrderedPage(eq(null), eq(pageSize));
        verify(reader).findOrderedPage(eq(firstPage.get(firstPage.size() - 1).getId()), eq(pageSize));
        // A short final page ends the scan: no extra round trip.
        verify(reader, times(2)).findOrderedPage(any(), anyInt());
    }

    @Test
    void emptySourceTable_reportsVerifiedWithoutWork() {
        when(reader.findOrderedPage(any(), anyInt())).thenReturn(List.of());
        when(reader.countAll()).thenReturn(0L);
        when(repository.count()).thenReturn(0L);

        var report = service.backfill();

        assertThat(report.getScanned()).isZero();
        assertThat(report.isVerified()).isTrue();
        assertThat(report.getChecksumMismatches()).isEmpty();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void pageSizeStaysBoundedAndRowKindsAreCounted() {
        int pageSize = LegacyAuditBackfillService.PAGE_SIZE;
        List<LegacyAuditRow> rows = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            rows.add(validRow(i == 0 ? "API_PROBE_SEARCH" : "CREATE"));
        }
        when(reader.findOrderedPage(any(), anyInt())).thenReturn(rows).thenReturn(List.of());
        stubBackfilledAfterInsert(rows);
        when(repository.count()).thenReturn((long) pageSize);
        when(reader.countAll()).thenReturn((long) pageSize);

        var report = service.backfill();

        assertThat(LegacyAuditBackfillService.PAGE_SIZE).isBetween(100, 10_000);
        assertThat(report.getScanned()).isEqualTo(pageSize);
        assertThat(report.getHttpRequestKind()).isEqualTo(1);
        assertThat(report.getAuditActionKind()).isEqualTo(pageSize - 1);
        assertThat(LegacyAuditChecksum.KIND_HTTP_REQUEST).isEqualTo("legacy.http.request");
    }
}