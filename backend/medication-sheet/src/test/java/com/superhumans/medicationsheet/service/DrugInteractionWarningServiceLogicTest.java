package com.superhumans.medicationsheet.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DrugInteractionWarningServiceLogicTest {

    private static PrescriptionDayPart part(Boolean planned, Boolean plannedFinished, Boolean completed) {
        PrescriptionDayPart p = new PrescriptionDayPart();
        p.setIsPlanned(planned);
        p.setIsPlannedFinished(plannedFinished);
        p.setIsCompleted(completed);
        return p;
    }

    @Test
    void isPlanned_requiresPlannedNotFinishedNotCompleted() {
        assertThat(DrugInteractionWarningService.isPlanned(part(true, false, false))).isTrue();
        assertThat(DrugInteractionWarningService.isPlanned(part(false, false, false))).isFalse();
        assertThat(DrugInteractionWarningService.isPlanned(part(true, true, false))).isFalse();
        assertThat(DrugInteractionWarningService.isPlanned(part(true, false, true))).isFalse();
        assertThat(DrugInteractionWarningService.isPlanned(part(null, null, null))).isFalse();
    }

    @Test
    void plannedPeriod_nullDaysIsEmpty() {
        PrescriptionItem item = new PrescriptionItem();
        assertThat(DrugInteractionWarningService.plannedPeriod(item)).isEmpty();
    }

    @Test
    void plannedPeriod_ignoresDeletedDaysAndNoPlannedParts() {
        PrescriptionItem item = new PrescriptionItem();
        PrescriptionItemDay deleted = new PrescriptionItemDay();
        deleted.setDayDate(LocalDate.of(2026, 1, 1));
        deleted.setDeleted(true);
        deleted.setDayParts(List.of(part(true, false, false)));

        PrescriptionItemDay unplanned = new PrescriptionItemDay();
        unplanned.setDayDate(LocalDate.of(2026, 1, 2));
        unplanned.setDeleted(false);
        unplanned.setDayParts(List.of(part(false, false, false)));

        PrescriptionItemDay planned = new PrescriptionItemDay();
        planned.setDayDate(LocalDate.of(2026, 1, 5));
        planned.setDeleted(false);
        planned.setDayParts(List.of(part(true, false, false)));

        item.setDays(List.of(deleted, unplanned, planned));
        assertThat(DrugInteractionWarningService.plannedPeriod(item))
                .contains(new LocalDate[]{LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 5)});
    }

    @Test
    void plannedPeriod_spansMinToMaxDate() {
        PrescriptionItem item = new PrescriptionItem();
        PrescriptionItemDay d1 = new PrescriptionItemDay();
        d1.setDayDate(LocalDate.of(2026, 1, 10));
        d1.setDeleted(false);
        d1.setDayParts(List.of(part(true, false, false)));
        PrescriptionItemDay d2 = new PrescriptionItemDay();
        d2.setDayDate(LocalDate.of(2026, 1, 1));
        d2.setDeleted(false);
        d2.setDayParts(List.of(part(true, true, false), part(true, false, false)));
        item.setDays(List.of(d1, d2));
        assertThat(DrugInteractionWarningService.plannedPeriod(item))
                .contains(new LocalDate[]{LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10)});
    }

    @Test
    void overlap_touchingEndsInclusive() {
        assertThat(DrugInteractionWarningService.overlap(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 10))).isEmpty();
        assertThat(DrugInteractionWarningService.overlap(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 10)))
                .contains(new LocalDate[]{LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 5)});
        assertThat(DrugInteractionWarningService.overlap(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 5)))
                .contains(new LocalDate[]{LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 5)});
        Optional<LocalDate[]> none = DrugInteractionWarningService.overlap(
                LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 9),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5));
        assertThat(none).isEmpty();
    }
}
