package com.superhumans.medicationsheet.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.List;
import java.util.Map;

/**
 * Admin browse view of the imported drug-interactions dataset (issue #305).
 * Companion to the import report: full drug list (small) plus a paginated
 * pair window, so the «База взаємодій» tab can render the stored base.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DrugInteractionCatalogResponse {
    Summary summary;
    List<DrugRow> drugs;
    CatalogPage page;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Summary {
        int drugs;
        long interactions;
        Map<String, Long> bySeverity;
        String lastImportAt;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DrugRow {
        String atcCode;
        String ukrainianRaw;
        String genericEn;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PairRow {
        String drugAAtc;
        String drugBAtc;
        String severity;
        String interaction;
        String interactionId;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CatalogPage {
        List<PairRow> content;
        long totalElements;
        int totalPages;
    }
}
