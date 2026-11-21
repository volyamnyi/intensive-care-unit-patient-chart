package com.superhumans.medicationsheet.pdf;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrescriptionPdfPagePlannerTest {

    private static final PrescriptionPdfPagePlanner PLANNER = new PrescriptionPdfPagePlanner();
    private static final PrescriptionPdfPagePlan.Header HEADER = new PrescriptionPdfPagePlan.Header(
            "Тестовий заклад", "00123456", null, null, false, "Синтетичний пацієнт", "");

    private static UUID id(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(Charset.defaultCharset()));
    }

    private static PrescriptionPdfSnapshot.Part part(String period, boolean planned, boolean cancelled,
            boolean completed, boolean finished, String dose, UUID dayId) {
        return new PrescriptionPdfSnapshot.Part(id(dayId + ":" + period), dayId, period, dose,
                planned, cancelled, completed, finished, false);
    }

    private static PrescriptionPdfSnapshot.Execution event(UUID partId, String login, String status, LocalDateTime administeredAt) {
        return new PrescriptionPdfSnapshot.Execution(id("event" + partId + login), partId,
                id(login), status, null, administeredAt, null, false, null, null);
    }

    private static PrescriptionPdfSnapshot snapshot(List<PrescriptionPdfSnapshot.Row> rows, LocalDate anchor) {
        return new PrescriptionPdfSnapshot(anchor, rows);
    }

    private static PrescriptionPdfSnapshot.Order order(int number) {
        return new PrescriptionPdfSnapshot.Order(id("order" + number), number, "Препарат " + number, null, null);
    }

    private static List<PrescriptionPdfPagePlan> plan(PrescriptionPdfSnapshot snap, Map<UUID, String> logins) {
        return PLANNER.plan(snap, HEADER, logins, null, null);
    }

    @Test
    void calendarWeeksSelectOnlyTheirOrders() {
        var a = order(1);
        var b = order(2);
        var start = LocalDate.of(2026, 10, 16);
        List<PrescriptionPdfSnapshot.Row> rows = List.of(
                row(a, start.plusDays(6), "night", true),
                row(b, start.plusDays(8), "morning", true));

        List<PrescriptionPdfPagePlan> pages = plan(snapshot(rows, start), Map.of());

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).week()).isEqualTo(start);
        assertThat(pages.get(0).rows()).singleElement().satisfies(r -> {
            assertThat(r.block().itemId()).isEqualTo(a.id());
            assertThat(r.block().owners()).containsOnlyKeys(27);
        });
        assertThat(pages.get(1).week()).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(pages.get(1).rows()).singleElement().satisfies(r -> {
            assertThat(r.block().itemId()).isEqualTo(b.id());
            assertThat(r.block().owners()).containsOnlyKeys(4);
        });
    }

    @Test
    void explicitRangeAnchorsAtFromAndIncludesBothBounds() {
        var a = order(1);
        var start = LocalDate.of(2026, 10, 20);
        var end = start.plusDays(7);
        var snap = snapshot(List.of(row(a, start.minusDays(1), "morning", true),
                row(a, start, "day", true), row(a, end, "night", true),
                row(a, end.plusDays(1), "evening", true)), start.minusDays(1));

        var pages = PLANNER.plan(snap, HEADER, Map.of(), start, end);
        assertThat(pages).extracting(PrescriptionPdfPagePlan::week).containsExactly(start, end);
        assertThat(pages.getFirst().rows().getFirst().block().owners()).containsOnlyKeys(1);
        assertThat(pages.getLast().rows().getFirst().block().owners()).containsOnlyKeys(3);
    }

    @Test
    void emptyListHasNoPagesAndBlankDaysHaveNoClinicalRows() {
        assertThat(plan(snapshot(List.of(), LocalDate.of(2026, 10, 16)), Map.of())).isEmpty();

        var a = order(1);
        var blankDay = id("blank");
        List<PrescriptionPdfSnapshot.Row> rows = List.of(row(a, LocalDate.of(2026, 10, 16), "morning",
                false, false, false, false, null, blankDay));
        assertThat(plan(snapshot(rows, LocalDate.of(2026, 10, 16)), Map.of())).isEmpty();
    }

    @Test
    void twelvePairsOnePageThirteenTwoPages() {
        var start = LocalDate.of(2026, 10, 16);
        List<PrescriptionPdfSnapshot.Row> rows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            rows.add(row(order(i + 1), start, "morning", true));
        }
        assertThat(plan(snapshot(rows, start), Map.of())).hasSize(1);

        rows.add(row(order(13), start, "morning", true));
        var pages = plan(snapshot(rows, start), Map.of());
        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).rows()).hasSize(12);
        assertThat(pages.get(1).rows()).singleElement().satisfies(r -> assertThat(r.block().number()).isEqualTo(13));
    }

    @Test
    void independentPlanAndFactDoNotInventEachOther() {
        var o = order(1);
        var day = id("day");
        var p = part("morning", false, false, true, false, "5", day);
        var rows = List.of(new PrescriptionPdfSnapshot.Row(o, LocalDate.of(2026, 10, 16), p, null));

        var page = plan(snapshot(rows, LocalDate.of(2026, 10, 16)), Map.of()).getFirst();

        var cell = page.rows().getFirst().block().owners().get(0);
        assertThat(cell).isNotNull();
        assertThat(cell.plan()).isEmpty();
        assertThat(cell.done()).isTrue();
        assertThat(page.signatures()).hasSize(28).containsOnly("");
    }

    @Test
    void cancelledPlanWithCompletedFactKeepsBothMarks() {
        var o = order(1);
        var day = id("day");
        var p = part("night", true, true, true, false, "5", day);
        var rows = List.of(new PrescriptionPdfSnapshot.Row(o, LocalDate.of(2026, 10, 16), p, null));

        var cell = plan(snapshot(rows, LocalDate.of(2026, 10, 16)), Map.of()).getFirst()
                .rows().getFirst().block().owners().get(3);

        assertThat(cell.plan()).isEqualTo("×");
        assertThat(cell.done()).isTrue();
    }

    @Test
    void executorComesOnlyFromCompletedPrimaryExecutor() {
        var o = order(1);
        var day = id("day");
        var p = part("night", false, false, false, false, null, day);
        var login = "nurse1";
        var date = LocalDate.of(2026, 10, 16);
        var afterMidnight = date.plusDays(1).atTime(0, 5, 30);
        var rows = List.of(new PrescriptionPdfSnapshot.Row(o, date, p, event(p.id(), login, "Completed", afterMidnight)),
                new PrescriptionPdfSnapshot.Row(o, date, p, event(p.id(), "other", "Pending", null)));

        var page = plan(snapshot(rows, date), Map.of(id(login), login, id("other"), "other")).getFirst();

        assertThat(page.signatures()).hasSize(28);
        assertThat(page.signatures().get(3)).isEqualTo(login);
        assertThat(page.signatures()).filteredOn(s -> !s.isEmpty()).containsExactly(login);
        var block = page.rows().getFirst().block();
        assertThat(block.owners()).containsOnlyKeys(3);
        assertThat(block.owners().get(3).plan()).isEmpty();
        assertThat(block.owners().get(3).done()).isTrue();
        assertThat(block.details()).anySatisfy(s -> assertThat(s)
                .contains("16.10.2026 / Ніч", "введено 17.10.2026 00:05:30"));
    }

    @Test
    void reversedOrPartialRangeIsRejected() {
        var o = order(1);
        var start = LocalDate.of(2026, 10, 16);
        var snap = snapshot(List.of(row(o, start, "morning", true)), start);
        var header = new PrescriptionPdfPagePlan.Header("X", "00123456", null, null, false, "", "");
        assertThatThrownBy(() -> PLANNER.plan(snap, header, Map.of(), null, start))
                .isInstanceOf(com.superhumans.exception.BadRequestException.class);
        assertThatThrownBy(() -> PLANNER.plan(snap, header, Map.of(), start.plusDays(1), start))
                .isInstanceOf(com.superhumans.exception.BadRequestException.class);
    }

    private static PrescriptionPdfSnapshot.Row row(PrescriptionPdfSnapshot.Order order, LocalDate date, String period,
            boolean planned) {
        return row(order, date, period, planned, false, false, false, null, id(order.id() + ":" + date));
    }

    private static PrescriptionPdfSnapshot.Row row(PrescriptionPdfSnapshot.Order order, LocalDate date, String period,
            boolean planned, boolean cancelled, boolean completed, boolean finished, String dose, UUID day) {
        var p = part(period, planned, cancelled, completed, finished, dose, day);
        return new PrescriptionPdfSnapshot.Row(order, date, p, null);
    }
}
