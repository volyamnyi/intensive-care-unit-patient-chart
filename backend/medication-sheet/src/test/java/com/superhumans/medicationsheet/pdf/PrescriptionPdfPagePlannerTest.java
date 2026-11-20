package com.superhumans.medicationsheet.pdf;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrescriptionPdfPagePlannerTest {

    private static final PrescriptionPdfPagePlanner PLANNER = new PrescriptionPdfPagePlanner();

    private static UUID id(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(Charset.defaultCharset()));
    }

    private static PrescriptionPdfSnapshot.Part part(String period, boolean planned, boolean cancelled,
            boolean completed, boolean finished, String dose, UUID dayId) {
        return new PrescriptionPdfSnapshot.Part(id(period + dayId + dose), dayId, period, dose,
                planned, cancelled, completed, finished, false);
    }

    private static PrescriptionPdfSnapshot.Execution event(UUID partId, String login, String status) {
        return new PrescriptionPdfSnapshot.Execution(id("event" + partId + login), partId,
                UUID.nameUUIDFromBytes(login.getBytes(Charset.defaultCharset())), status, "5", null, null, false, null, null);
    }

    private static PrescriptionPdfSnapshot snapshot(List<PrescriptionPdfSnapshot.Row> rows, LocalDate anchor) {
        return new PrescriptionPdfSnapshot(anchor, rows);
    }

    private static PrescriptionPdfSnapshot.Order order(int number) {
        return new PrescriptionPdfSnapshot.Order(id("order" + number), number, "Препарат " + number, null, null);
    }

    private static List<PrescriptionPdfPagePlan> plan(PrescriptionPdfSnapshot snap, Map<UUID, String> logins) {
        return PLANNER.plan(snap, new PrescriptionPdfPagePlan.Header("Тестовий заклад", "00123456",
                null, null, false, "Синтетичний пацієнт", ""), logins, null, null);
    }

    @Test
    void calendarWeeksSelectOnlyTheirOrders() {
        var a = order(1);
        var b = order(2);
        var start = LocalDate.of(2026, 10, 16);
        List<PrescriptionPdfSnapshot.Row> rows = List.of(
                row(a, start, "morning", true),
                row(b, start.plusDays(8), "morning", true));

        List<PrescriptionPdfPagePlan> pages = plan(snapshot(rows, start), Map.of());

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).week()).isEqualTo(start);
        assertThat(pages.get(0).rows()).singleElement().satisfies(r -> assertThat(r.block().number()).isEqualTo(1));
        assertThat(pages.get(1).week()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(pages.get(1).pageIndex()).isEqualTo(1);
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
    }

    @Test
    void cancelledPlanWithCompletedFactKeepsBothMarks() {
        var o = order(1);
        var day = id("day");
        var p = part("night", true, true, true, false, "5", day);
        var rows = List.of(new PrescriptionPdfSnapshot.Row(o, LocalDate.of(2026, 10, 16), p, null));

        var cell = plan(snapshot(rows, LocalDate.of(2026, 10, 16)), Map.of()).getFirst()
                .rows().getFirst().block().owners().get(24);

        assertThat(cell.plan()).isEqualTo("×");
        assertThat(cell.done()).isTrue();
    }

    @Test
    void executorComesOnlyFromCompletedPrimaryExecutor() {
        var o = order(1);
        var day = id("day");
        var partId = id("part");
        var p = part("morning", false, false, false, false, null, day);
        var login = "nurse1";
        var key = UUID.nameUUIDFromBytes(login.getBytes(Charset.defaultCharset()));
        var rows = List.of(new PrescriptionPdfSnapshot.Row(o, LocalDate.of(2026, 10, 16), p,
                event(partId, login, "Completed")));

        var pages = plan(snapshot(rows, LocalDate.of(2026, 10, 16)), Map.of(key, login));

        assertThat(pages.getFirst().signatures().get(0)).isEqualTo(login);
        assertThat(pages.getFirst().footerNotes()).anyMatch(note -> note.contains("введено"));
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
        return row(order, date, period, planned, false, false, false, null, id(date.toString() + period));
    }

    private static PrescriptionPdfSnapshot.Row row(PrescriptionPdfSnapshot.Order order, LocalDate date, String period,
            boolean planned, boolean cancelled, boolean completed, boolean finished, String dose, UUID day) {
        var p = part(period, planned, cancelled, completed, finished, dose, day);
        return new PrescriptionPdfSnapshot.Row(order, date, p, null);
    }
}
