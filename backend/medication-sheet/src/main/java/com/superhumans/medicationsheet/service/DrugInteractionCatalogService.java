package com.superhumans.medicationsheet.service;

import com.superhumans.exception.BadRequestException;
import com.superhumans.medicationsheet.dto.DrugInteractionCatalogResponse;
import com.superhumans.medicationsheet.dto.DrugInteractionCatalogResponse.CatalogPage;
import com.superhumans.medicationsheet.dto.DrugInteractionCatalogResponse.DrugRow;
import com.superhumans.medicationsheet.dto.DrugInteractionCatalogResponse.PairRow;
import com.superhumans.medicationsheet.dto.DrugInteractionCatalogResponse.Summary;
import com.superhumans.medicationsheet.entity.DrugInteractionDrug;
import com.superhumans.medicationsheet.entity.DrugInteractionPair;
import com.superhumans.medicationsheet.repository.DrugInteractionDrugRepository;
import com.superhumans.medicationsheet.repository.DrugInteractionPairRepository;
import com.superhumans.repository.core.SystemSettingsRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only browse view of the imported drug-interactions dataset (issue #305).
 * Backs the «База взаємодій» admin tab: full drug list (small) plus a
 * paginated, severity-/query-filtered pair window. Never blocks planning.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DrugInteractionCatalogService {

    DrugInteractionDrugRepository drugRepository;
    DrugInteractionPairRepository pairRepository;
    SystemSettingsRepository settingsRepository;

    @Transactional(readOnly = true, transactionManager = "medTransactionManager")
    public DrugInteractionCatalogResponse getCatalog(String severity, String query, Pageable pageable) {
        String sev = (severity == null || severity.isBlank()) ? null : severity.trim().toLowerCase();
        if (sev != null && !DrugInteractionImportService.SEVERITIES.contains(sev)) {
            throw new BadRequestException("Невідомий severity фільтра: " + severity);
        }
        String q = (query == null || query.isBlank()) ? null : query.trim();

        List<DrugRow> drugs = drugRepository.findAllOrderByAtcCodeAsc().stream()
                .map(this::toDrugRow)
                .toList();
        Page<DrugInteractionPair> page = pairRepository.findCatalog(sev, q, pageable);

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (String s : List.of("low", "medium", "high", "critical")) {
            bySeverity.put(s, 0L);
        }
        for (Object[] row : pairRepository.countBySeverity()) {
            bySeverity.put(String.valueOf(row[0]), (Long) row[1]);
        }
        String lastImportAt = settingsRepository.findByKey(DrugInteractionImportService.LAST_IMPORT_KEY)
                .map(s -> s.getValue())
                .orElse(null);

        List<PairRow> content = page.getContent().stream().map(this::toPairRow).toList();
        return DrugInteractionCatalogResponse.builder()
                .summary(Summary.builder()
                        .drugs(drugs.size())
                        .interactions(pairRepository.count())
                        .bySeverity(bySeverity)
                        .lastImportAt(lastImportAt)
                        .build())
                .drugs(drugs)
                .page(CatalogPage.builder()
                        .content(content)
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    private DrugRow toDrugRow(DrugInteractionDrug d) {
        return DrugRow.builder()
                .atcCode(d.getAtcCode())
                .ukrainianRaw(d.getUkrainianRaw())
                .genericEn(d.getGenericEn())
                .build();
    }

    private PairRow toPairRow(DrugInteractionPair p) {
        return PairRow.builder()
                .drugAAtc(p.getDrugAAtc())
                .drugBAtc(p.getDrugBAtc())
                .severity(p.getSeverity())
                .interaction(p.getInteraction())
                .interactionId(p.getInteractionId())
                .build();
    }
}
