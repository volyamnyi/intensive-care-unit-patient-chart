package com.superhumans.medicationsheet.migration;

import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/** Mutable counters for one import run (logged and stored as JSON). */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportCounters {

    int listsCreated;
    int listsSkippedMapped;
    int listsQuarantined;
    int items;
    int days;
    int parts;
    int elementsQuarantined;
    int nameReports;
    int vitListsMerged;
    int vitEntriesInserted;
    int vitEntriesSkipped;
    int vitEntriesQuarantined;
    int auditFailures;
    final Set<String> missingPatients = new HashSet<>();

    public void listCreated() {
        listsCreated++;
    }

    public void listSkippedMapped() {
        listsSkippedMapped++;
    }

    public void listQuarantined() {
        listsQuarantined++;
    }

    public void addItems(int items, int days, int parts, int quarantinedElements, int nameReports) {
        this.items += items;
        this.days += days;
        this.parts += parts;
        this.elementsQuarantined += quarantinedElements;
        this.nameReports += nameReports;
    }

    public void addVitalMerge(int inserted, int skipped, int quarantined) {
        vitListsMerged++;
        vitEntriesInserted += inserted;
        vitEntriesSkipped += skipped;
        vitEntriesQuarantined += quarantined;
    }

    public void auditFailure() {
        auditFailures++;
    }

    /** Minimal JSON object (integer fields only, no escaping concerns). */
    public String toJson() {
        return "{\"listsCreated\":" + listsCreated
                + ",\"listsSkippedMapped\":" + listsSkippedMapped
                + ",\"listsQuarantined\":" + listsQuarantined
                + ",\"items\":" + items
                + ",\"days\":" + days
                + ",\"parts\":" + parts
                + ",\"elementsQuarantined\":" + elementsQuarantined
                + ",\"nameReports\":" + nameReports
                + ",\"vitListsMerged\":" + vitListsMerged
                + ",\"vitEntriesInserted\":" + vitEntriesInserted
                + ",\"vitEntriesSkipped\":" + vitEntriesSkipped
                + ",\"vitEntriesQuarantined\":" + vitEntriesQuarantined
                + ",\"missingPatients\":" + missingPatients.size()
                + ",\"auditFailures\":" + auditFailures + "}";
    }
}
