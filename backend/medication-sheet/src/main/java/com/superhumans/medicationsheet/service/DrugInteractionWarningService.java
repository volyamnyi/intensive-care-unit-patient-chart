package com.superhumans.medicationsheet.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.dto.PrescriptionInteractionsResponse.ItemWarning;
import com.superhumans.medicationsheet.dto.PrescriptionInteractionsResponse.MissingAtcNotice;
import com.superhumans.medicationsheet.dto.PrescriptionInteractionsResponse.PairWarning;
import com.superhumans.medicationsheet.dto.PrescriptionInteractionsResponse;
import com.superhumans.medicationsheet.entity.DrugInteractionPair;
import com.superhumans.medicationsheet.entity.DrugInteractionDrug;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.repository.DrugInteractionDrugRepository;
import com.superhumans.medicationsheet.repository.DrugInteractionPairRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemRepository;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Server-side drug-interaction warnings for a prescription list (issue #304).
 *
 * <p>A medicine "planned" period is {@code [min(dayDate), max(dayDate)]}
 * across its days that hold at least one planned day part
 * ({@code isPlanned && !isPlannedFinished && !isCompleted}). Warnings are
 * computed on read: dangerous pairs ({@code medium/high/critical}) whose ATC
 * set intersects the list's planned medics and whose periods overlap
 * ({@code aStart <= bEnd && bStart <= aEnd}).
 *
 * <p>Read-only and never blocking: planning endpoints do not consult this
 * service. The frontend refetches after every mutation, so warnings refresh
 * with date changes automatically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DrugInteractionWarningService {

    static final List<String> DANGEROUS_SEVERITIES = List.of("medium", "high", "critical");

    PrescriptionItemRepository itemRepository;
    DrugInteractionPairRepository pairRepository;
    DrugInteractionDrugRepository drugRepository;
    PrescriptionListRepository listRepository;

    public static boolean isPlanned(PrescriptionDayPart part) {
        return Boolean.TRUE.equals(part.getIsPlanned())
                && !Boolean.TRUE.equals(part.getIsPlannedFinished())
                && !Boolean.TRUE.equals(part.getIsCompleted());
    }

    public static Optional<LocalDate[]> plannedPeriod(PrescriptionItem item) {
        if (item.getDays() == null) {
            return Optional.empty();
        }
        LocalDate start = null;
        LocalDate end = null;
        for (PrescriptionItemDay day : item.getDays()) {
            if (Boolean.TRUE.equals(day.getDeleted()) || day.getDayParts() == null) {
                continue;
            }
            boolean hasPlanned = day.getDayParts().stream().anyMatch(DrugInteractionWarningService::isPlanned);
            if (!hasPlanned || day.getDayDate() == null) {
                continue;
            }
            if (start == null || day.getDayDate().isBefore(start)) {
                start = day.getDayDate();
            }
            if (end == null || day.getDayDate().isAfter(end)) {
                end = day.getDayDate();
            }
        }
        return (start == null) ? Optional.empty() : Optional.of(new LocalDate[]{start, end});
    }

    /** Inclusive overlap of [aStart, aEnd] and [bStart, bEnd]; empty when none. */
    public static Optional<LocalDate[]> overlap(LocalDate aStart, LocalDate aEnd,
                                                LocalDate bStart, LocalDate bEnd) {
        LocalDate start = aStart.isAfter(bStart) ? aStart : bStart;
        LocalDate end = aEnd.isBefore(bEnd) ? aEnd : bEnd;
        return start.isAfter(end) ? Optional.empty() : Optional.of(new LocalDate[]{start, end});
    }

    @Transactional(readOnly = true, transactionManager = "medTransactionManager")
    public PrescriptionInteractionsResponse computeWarnings(UUID listId) {
        listRepository.findById(listId)
                .orElseThrow(() -> new NotFoundException("List not found: " + listId));
        List<PrescriptionItem> items = itemRepository.findByListIdAndDeletedFalseOrderBySortOrderAsc(listId);
        if (items.isEmpty()) {
            // A freshly created list has no items yet — that is not an error.
            return PrescriptionInteractionsResponse.builder()
                    .warnings(List.of())
                    .missingAtc(MissingAtcNotice.builder().present(false).names(List.of()).build())
                    .build();
        }

        Map<UUID, LocalDate[]> periods = new HashMap<>();
        for (PrescriptionItem item : items) {
            plannedPeriod(item).ifPresent(p -> periods.put(item.getId(), p));
        }

        Map<String, DrugInteractionDrug> atcToDrug = new HashMap<>();
        Set<String> atcKeys = new HashSet<>();
        for (PrescriptionItem item : items) {
            if (item.getMedicineAtcCode() != null && !item.getMedicineAtcCode().isBlank()) {
                atcKeys.add(item.getMedicineAtcCode());
            }
        }
        for (String atc : atcKeys) {
            drugRepository.findByAtcCode(atc).ifPresent(d -> atcToDrug.put(atc, d));
        }

        Map<UUID, List<PairWarning>> grouped = new HashMap<>();
        if (!periods.isEmpty()) {
            for (PrescriptionItem a : items) {
                if (a.getMedicineAtcCode() == null || a.getMedicineAtcCode().isBlank()) {
                    continue;
                }
                LocalDate[] pa = periods.get(a.getId());
                List<DrugInteractionPair> pairs = pairRepository.findDangerousForAtc(a.getMedicineAtcCode(), DANGEROUS_SEVERITIES);
                Map<String, List<DrugInteractionPair>> byOpponentAtc = new HashMap<>();
                for (DrugInteractionPair pair : pairs) {
                    String otherAtc = pair.getDrugAAtc().equals(a.getMedicineAtcCode())
                            ? pair.getDrugBAtc() : pair.getDrugAAtc();
                    byOpponentAtc.computeIfAbsent(otherAtc, k -> new ArrayList<>()).add(pair);
                }
                Set<String> checked = new HashSet<>();
                for (PrescriptionItem b : items) {
                if (b.getId().equals(a.getId())
                        || b.getMedicineAtcCode() == null || b.getMedicineAtcCode().isBlank()
                        || checked.contains(b.getMedicineAtcCode())
                        || !byOpponentAtc.containsKey(b.getMedicineAtcCode())) {
                    continue;
                }
                    checked.add(b.getMedicineAtcCode());
                    List<DrugInteractionPair> matched = byOpponentAtc.get(b.getMedicineAtcCode());
                    Optional<LocalDate[]> ov = overlap(pa[0], pa[1],
                            periods.get(b.getId())[0], periods.get(b.getId())[1]);
                    if (ov.isEmpty()) {
                        continue;
                    }
                    PairWarning toB = PairWarning.builder()
                            .otherItemId(b.getId())
                            .otherNameUk(datasetName(atcToDrug, b.getMedicineAtcCode(), b.getMedicineName()))
                            .severity(matched.get(0).getSeverity())
                            .interactionText(joinTexts(matched))
                            .overlapStart(ov.get()[0])
                            .overlapEnd(ov.get()[1])
                            .interactionIds(matched.stream().map(DrugInteractionPair::getInteractionId).distinct().toList())
                            .build();
                    PairWarning toA = PairWarning.builder()
                            .otherItemId(a.getId())
                            .otherNameUk(datasetName(atcToDrug, a.getMedicineAtcCode(), a.getMedicineName()))
                            .severity(toB.getSeverity())
                            .interactionText(toB.getInteractionText())
                            .overlapStart(ov.get()[0])
                            .overlapEnd(ov.get()[1])
                            .interactionIds(toB.getInteractionIds())
                            .build();
                    grouped.computeIfAbsent(a.getId(), k -> new ArrayList<>()).add(toB);
                    grouped.computeIfAbsent(b.getId(), k -> new ArrayList<>()).add(toA);
                }
            }
        }

        List<ItemWarning> warnings = new ArrayList<>();
        for (PrescriptionItem item : items) {
            List<PairWarning> pairs = grouped.get(item.getId());
            if (pairs != null) {
                warnings.add(ItemWarning.builder()
                        .itemId(item.getId())
                        .nameUk(item.getMedicineName())
                        .interactions(pairs)
                        .build());
            }
        }

        List<String> missingNames = items.stream()
                .filter(i -> periods.containsKey(i.getId()))
                .filter(i -> i.getMedicineAtcCode() == null || i.getMedicineAtcCode().isBlank())
                .map(PrescriptionItem::getMedicineName)
                .sorted(Comparator.naturalOrder())
                .distinct()
                .toList();

        MissingAtcNotice missingAtc = MissingAtcNotice.builder()
                .present(!missingNames.isEmpty())
                .names(missingNames)
                .build();

        log.info("Interaction warnings: listId={}, plannedItems={}, warnedItems={}",
                listId, periods.size(), warnings.size());
        return PrescriptionInteractionsResponse.builder()
                .warnings(warnings)
                .missingAtc(missingAtc)
                .build();
    }

    /** Dataset {@code ukrainian_raw} wins for partner display; falls back to the stored item name. */
    private static String datasetName(Map<String, DrugInteractionDrug> atcToDrug, String atc, String fallback) {
        DrugInteractionDrug drug = (atc == null) ? null : atcToDrug.get(atc);
        if (drug != null && drug.getUkrainianRaw() != null && !drug.getUkrainianRaw().isBlank()) {
            return drug.getUkrainianRaw();
        }
        return fallback;
    }

    private static String joinTexts(List<DrugInteractionPair> pairs) {
        return pairs.stream()
                .map(DrugInteractionPair::getInteraction)
                .distinct()
                .reduce((x, y) -> x + " / " + y)
                .orElse("");
    }
}
