package com.superhumans.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Backfill run report: counts by kind plus counts/checksum verification (§H.4). */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LegacyBackfillReportResponse {
    int scanned;
    int inserted;
    int skippedExisting;
    int skippedInvalid;
    int httpRequestKind;
    int auditActionKind;
    long legacySourceCount;
    long backfilledCount;
    List<String> checksumMismatches;
    boolean verified;
}
