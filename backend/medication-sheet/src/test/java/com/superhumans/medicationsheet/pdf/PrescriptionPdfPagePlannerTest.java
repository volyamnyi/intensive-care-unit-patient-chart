package com.superhumans.medicationsheet.pdf;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionPdfPagePlannerTest {

    private static final PrescriptionPdfUsernameResolver RESOLVER = new PrescriptionPdfUsernameResolver();
    private static final PrescriptionPdfPagePlanner PLANNER = new PrescriptionPdfPagePlanner(RESOLVER);

    private static PrescriptionPdfSnapshot.Part part(String period) {
        return new PrescriptionPdfSnapshot.Part(
                UUID.randomUUID(), period, null, false, false, false, false, null, null);
    }

    private static PrescriptionPdfSnapshot.Part plannedPart(String period, String dose) {
        return new PrescriptionPdfSnapshot.Part(
                UUID.randomUUID(), period, dose, true, false, false, false, null, null);
    }

    private static PrescriptionPdfSnapshot.Day day(LocalDate date, PrescriptionPdfSnapshot.Part... parts) {
        return new PrescriptionPdfSnapshot.Day(UUID.randomUUID(), date, List.of(parts));
    }

    private static PrescriptionPdfSnapshot.Item item(
            int sortOrder, PrescriptionPdfSnapshot.Day... days) {
        return new PrescriptionPdfSnapshot.Item(
                UUID.randomUUID(), "Med" + sortOrder, "oral", "daily", sortOrder, List.of(days));
    }

    private static PrescriptionPdfSnapshot snapshot(
            PrescriptionPdfSnapshot.Item... items) {
        return new PrescriptionPdfSnapshot(
                UUID.randomUUID(), 1001L, null, 19L, "List", "Saved",
                List.of(items), Map.of());
    }

    private static PrescriptionPdfPagePlanner.HeaderData header() {
        return new PrescriptionPdfPagePlanner.HeaderData("Hosp", "123", "", "Patient", "");
    }

    private static List<PrescriptionPdfPagePlan> plan(PrescriptionPdfSnapshot snapshot) {
        return PLANNER.plan(snapshot, header(), Map.of(), "doctor1");
    }

    @Test
    void oneDayOnePeriodProducesOnePdf() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfSnapshot snap = snapshot(item(0,
                day(date, plannedPart("morning", "40 мг"))));

        List<PrescriptionPdfPagePlan> pages = plan(snap);

        assertThat(pages).hasSize(1);
        assertThat(pages.get(0).dates()).containsExactly(date);
        assertThat(pages.get(0).totalPages()).isEqualTo(1);
        assertThat(pages.get(0).continuation()).isFalse();
    }

    @Test
    void oneDayFourPeriodsProducesOnePdf() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfSnapshot snap = snapshot(item(0, day(date,
                plannedPart("morning", "10"), plannedPart("day", "10"),
                plannedPart("evening", "10"), plannedPart("night", "10"))));

        List<PrescriptionPdfPagePlan> pages = plan(snap);

        assertThat(pages).hasSize(1);
        assertThat(PrescriptionPdfPagePlanner.totalFilledPeriodCount(snap)).isEqualTo(4);
    }

    @Test
    void tenActiveDaysProduceOnePdf() {
        List<PrescriptionPdfSnapshot.Day> days = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            days.add(day(LocalDate.of(2026, 7, 1).plusDays(i), plannedPart("morning", "10")));
        }
        PrescriptionPdfSnapshot snap = snapshot(item(0, days.toArray(new PrescriptionPdfSnapshot.Day[0])));

        assertThat(plan(snap)).hasSize(1);
    }

    @Test
    void elevenActiveDaysProduceTwoPdfs() {
        List<PrescriptionPdfSnapshot.Day> days = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            days.add(day(LocalDate.of(2026, 7, 1).plusDays(i), plannedPart("morning", "10")));
        }
        PrescriptionPdfSnapshot snap = snapshot(item(0, days.toArray(new PrescriptionPdfSnapshot.Day[0])));

        List<PrescriptionPdfPagePlan> pages = plan(snap);

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).dates()).hasSize(10);
        assertThat(pages.get(1).dates()).hasSize(1);
        assertThat(pages.get(1).continuation()).isTrue();
        assertThat(pages.get(0).totalPages()).isEqualTo(2);
    }

    @Test
    void twentyActiveDaysProduceTwoPdfsAndTwentyOneProduceThree() {
        List<PrescriptionPdfSnapshot.Day> twenty = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            twenty.add(day(LocalDate.of(2026, 7, 1).plusDays(i), plannedPart("morning", "10")));
        }
        assertThat(plan(snapshot(item(0, twenty.toArray(new PrescriptionPdfSnapshot.Day[0]))))).hasSize(2);

        List<PrescriptionPdfSnapshot.Day> twentyOne = new ArrayList<>(twenty);
        twentyOne.add(day(LocalDate.of(2026, 7, 21), plannedPart("morning", "10")));
        assertThat(plan(snapshot(item(0, twentyOne.toArray(new PrescriptionPdfSnapshot.Day[0]))))).hasSize(3);
    }

    @Test
    void nineItemsFitOnePageTenItemsNeedContinuation() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        List<PrescriptionPdfSnapshot.Item> nine = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            nine.add(item(i, day(date, plannedPart("morning", "10"))));
        }
        assertThat(plan(snapshot(nine.toArray(new PrescriptionPdfSnapshot.Item[0])))).hasSize(1);

        List<PrescriptionPdfSnapshot.Item> ten = new ArrayList<>(nine);
        ten.add(item(9, day(date, plannedPart("morning", "10"))));
        List<PrescriptionPdfPagePlan> pages = plan(snapshot(ten.toArray(new PrescriptionPdfSnapshot.Item[0])));

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).items()).hasSize(9);
        assertThat(pages.get(1).items()).hasSize(1);
        // Same date set on both pages (dates-major cartesian order).
        assertThat(pages.get(1).dates()).containsExactly(date);
    }

    @Test
    void nightOnlyCountsAsActive() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfSnapshot snap = snapshot(item(0, day(date, plannedPart("night", "10"))));

        assertThat(PrescriptionPdfPagePlanner.activeDates(snap)).containsExactly(date);
        assertThat(PrescriptionPdfPagePlanner.filledPeriodCount(date, snap)).isEqualTo(1);
    }

    @Test
    void emptyPartsWithoutDataAreNotActive() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfSnapshot snap = snapshot(item(0, day(date, part("morning"), part("night"))));

        assertThat(PrescriptionPdfPagePlanner.activeDates(snap)).isEmpty();
    }

    @Test
    void completedOnlyCountsAsActive() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfSnapshot.Part completed = new PrescriptionPdfSnapshot.Part(
                UUID.randomUUID(), "evening", "5", false, false, true, false, null, null);
        PrescriptionPdfSnapshot snap = snapshot(item(0, day(date, completed)));

        assertThat(PrescriptionPdfPagePlanner.activeDates(snap)).containsExactly(date);
        assertThat(PrescriptionPdfPagePlanner.filledPeriodCount(date, snap)).isEqualTo(1);
    }

    @Test
    void cancelledCountsAsActiveButResetCellDoesNot() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfSnapshot.Part cancelled = new PrescriptionPdfSnapshot.Part(
                UUID.randomUUID(), "morning", "10", true, true, false, false, null, null);
        PrescriptionPdfSnapshot snap = snapshot(item(0, day(date, cancelled, part("day"))));

        assertThat(PrescriptionPdfPagePlanner.filledPeriodCount(date, snap)).isEqualTo(1);
        assertThat(PrescriptionPdfPagePlanner.activeDates(snap)).containsExactly(date);
    }

    @Test
    void totallyEmptyDayIsNotActive() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfSnapshot snap = snapshot(item(0, day(date, part("morning"), part("day"))));

        assertThat(PrescriptionPdfPagePlanner.activeDates(snap)).isEmpty();
        assertThat(plan(snap)).hasSize(1);
        assertThat(plan(snap).get(0).dates()).isEmpty();
    }

    @Test
    void dateGapsAreKeptAsIsWithoutFilling() {
        LocalDate first = LocalDate.of(2026, 7, 1);
        LocalDate third = LocalDate.of(2026, 7, 3);
        PrescriptionPdfSnapshot snap = snapshot(item(0,
                day(first, plannedPart("morning", "10")),
                day(third, plannedPart("morning", "10"))));

        Set<LocalDate> active = PrescriptionPdfPagePlanner.activeDates(snap);

        assertThat(active).containsExactly(first, third);
        List<PrescriptionPdfPagePlan> pages = plan(snap);
        assertThat(pages).hasSize(1);
        assertThat(pages.get(0).dates()).containsExactly(first, third);
    }

    @Test
    void laterDatesOfOtherItemsAreNotLost() {
        PrescriptionPdfSnapshot snap = snapshot(
                item(0, day(LocalDate.of(2026, 7, 1), plannedPart("morning", "10"))),
                item(1, day(LocalDate.of(2026, 7, 15), plannedPart("morning", "10"))));

        assertThat(PrescriptionPdfPagePlanner.activeDates(snap))
                .containsExactly(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15));
    }

    @Test
    void doctorUsernameReachesEveryColumn() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfSnapshot snap = snapshot(item(0, day(date, plannedPart("morning", "10"))));

        List<PrescriptionPdfPagePlan> pages = PLANNER.plan(snap, header(), Map.of(), "likar1");

        assertThat(pages.get(0).doctorSignatureUsername()).isEqualTo("likar1");
        assertThat(pages.get(0).items().get(0).columns().get(0).doctorLine()).isEqualTo("likar1");
    }

    @Test
    void classifyPriorityFinishedOverCompletedOverCancelledOverPlanned() {
        UUID id = UUID.randomUUID();
        assertThat(PrescriptionPdfPagePlanner.classify(
                        new PrescriptionPdfSnapshot.Part(
                                id, "morning", "10", true, true, true, true, null, null),
                        List.of()))
                .isEqualTo(PeriodPrintState.COMPLETED_FINISHED);
        assertThat(PrescriptionPdfPagePlanner.classify(
                        new PrescriptionPdfSnapshot.Part(
                                id, "morning", "10", true, true, true, false, null, null),
                        List.of()))
                .isEqualTo(PeriodPrintState.COMPLETED);
        assertThat(PrescriptionPdfPagePlanner.classify(
                        new PrescriptionPdfSnapshot.Part(
                                id, "morning", "10", true, true, false, false, null, null),
                        List.of()))
                .isEqualTo(PeriodPrintState.CANCELLED);
        assertThat(PrescriptionPdfPagePlanner.classify(
                        new PrescriptionPdfSnapshot.Part(
                                id, "morning", "10", true, false, false, false, null, null),
                        List.of()))
                .isEqualTo(PeriodPrintState.PLANNED);
    }

    @Test
    void executionsAloneMakePeriodCompleted() {
        PrescriptionPdfSnapshot.Part part = part("morning");
        Map<UUID, List<PrescriptionPdfSnapshot.Execution>> execs = new HashMap<>();
        execs.put(part.partId(), List.of(new PrescriptionPdfSnapshot.Execution(
                UUID.randomUUID(), null, "5", "Completed", null, null)));
        PrescriptionPdfSnapshot snap = new PrescriptionPdfSnapshot(
                UUID.randomUUID(), 1001L, null, 19L, "L", "Saved",
                List.of(item(0, new PrescriptionPdfSnapshot.Day(
                        UUID.randomUUID(), LocalDate.of(2026, 7, 25), List.of(part)))),
                execs);

        assertThat(PrescriptionPdfPagePlanner.classify(part, execs.get(part.partId())))
                .isEqualTo(PeriodPrintState.COMPLETED);
        assertThat(PrescriptionPdfPagePlanner.filledPeriodCount(LocalDate.of(2026, 7, 25), snap))
                .isEqualTo(1);
    }
}
