package com.superhumans.medicationsheet.pdf;

import com.superhumans.exception.BadRequestException;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfPagePlan.Block;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfPagePlan.Geometry;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfPagePlan.Header;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfPagePlan.Placed;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfSnapshot.Execution;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

import static com.superhumans.medicationsheet.pdf.PrescriptionPdfLayout.*;

@Service
public final class PrescriptionPdfPagePlanner {
    private record Footer(List<String> signatures, List<String> notes) { }

    public List<PrescriptionPdfPagePlan> plan(PrescriptionPdfSnapshot snapshot, Header header,
            Map<UUID, String> logins, LocalDate from, LocalDate to) {
        if ((from == null) != (to == null) || (from != null && from.isAfter(to))) {
            throw new BadRequestException("Передайте обидві межі періоду в календарному порядку.");
        }
        var weeks = PrescriptionPdfPrintModel.weeks(snapshot, from, to);
        if (weeks.isEmpty()) {
            return List.of();
        }
        PrescriptionPdfLayout metrics = new PrescriptionPdfLayout();
        List<PrescriptionPdfPagePlan> pages = new ArrayList<>();
        weeks.forEach((week, orders) -> {
            Deque<Block> queue = new ArrayDeque<>();
            int[] number = {0};
            orders.forEach((order, cells) -> queue.add(metrics.measure(PrescriptionPdfPrintModel.group(
                    order, ++number[0], week, cells, logins))));
            while (!queue.isEmpty()) {
                pages.add(nextPage(queue, pages.size(), week, header, logins, metrics));
            }
        });
        return List.copyOf(pages);
    }

    private PrescriptionPdfPagePlan nextPage(Deque<Block> queue, int index, LocalDate week, Header header,
            Map<UUID, String> logins, PrescriptionPdfLayout metrics) {
        Geometry g = metrics.geometry(header, index);
        float budget = g.footerTop() - g.bodyTop();
        float used = 0;
        List<Block> accepted = new ArrayList<>();
        Footer acceptedFooter = null;
        while (!queue.isEmpty() && accepted.size() < PAIRS) {
            Block b = queue.peek();
            accepted.add(b);
            Footer footer = footer(accepted, index, week, logins, metrics);
            if (used + height(b) + textHeight(footer.notes()) <= budget + 0.001f) {
                used += height(b);
                queue.remove();
                acceptedFooter = footer;
            } else {
                accepted.removeLast();
                if (accepted.isEmpty()) {
                    queue.remove();
                    accepted.add(split(b, budget, footer.notes(), queue, metrics));
                    acceptedFooter = new Footer(footer.signatures(), List.of());
                }
                break;
            }
        }
        List<Placed> placed = new ArrayList<>();
        float y = g.bodyTop();
        for (Block b : accepted) {
            placed.add(new Placed(b, y, pairHeight(b), height(b)));
            y += height(b);
        }
        return new PrescriptionPdfPagePlan(index, week, header, g, List.copyOf(placed),
                acceptedFooter.signatures(), acceptedFooter.notes());
    }

    /** Only the first fragment owns marks/signatures. Each tail consumes new, already measured lines. */
    private Block split(Block b, float budget, List<String> notes, Deque<Block> queue, PrescriptionPdfLayout metrics) {
        List<String> lines = new ArrayList<>();
        List<String> label = b.label();
        String key = "Позиція №" + b.number();
        if (pairHeight(b) + LEADING + 2 * PAD > budget) {
            lines.addAll(label);
            label = List.of(key);
        } else if (!b.owners().isEmpty()) {
            // Keep a printed key on both halves even when two orders have the same name.
            lines.addAll(label);
            label = List.of(key);
        }
        lines.addAll(b.details());
        lines.addAll(notes);
        int fit = (int) Math.floor((budget - Math.max(PAIR, textHeight(label)) - 2 * PAD) / LEADING);
        if (fit < 1 || lines.isEmpty()) {
            throw overflow();
        }
        int cut = Math.min(fit, lines.size());
        if (cut < lines.size()) {
            queue.addFirst(new Block(b.itemId(), b.number(), metrics.wrap(key + " (продовження)", NAME - 2 * PAD),
                    List.copyOf(lines.subList(cut, lines.size())), Map.of()));
        }
        return new Block(b.itemId(), b.number(), label, List.copyOf(lines.subList(0, cut)), b.owners());
    }

    private Footer footer(List<Block> blocks, int page, LocalDate week, Map<UUID, String> logins, PrescriptionPdfLayout metrics) {
        List<String> signatures = new ArrayList<>(SLOTS);
        List<String> notes = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            List<Execution> events = new ArrayList<>();
            for (Block b : blocks) {
                var cell = b.owners().get(slot);
                if (cell != null) {
                    events.addAll(cell.events());
                }
            }
            String names = String.join(", ", events.stream().filter(Execution::confirmed).sorted(Execution.CHRONOLOGY)
                    .filter(e -> e.executedBy() != null).map(e -> logins.get(e.executedBy()))
                    .filter(PrescriptionPdfSnapshot::present).distinct().toList());
            if (metrics.font.getWidth(names, SIGN) > SLOT - 2 * PAD) {
                String ref = "С" + (slot + 1);
                String context = "Підписи " + ref + " (арк. " + (page + 1) + ", "
                        + PrescriptionPdfPrintModel.DAY.format(week.plusDays(slot / 4)) + " / "
                        + PrescriptionPdfPeriod.LABELS.get(slot % 4) + "): ";
                notes.addAll(metrics.wrap(context + names, WIDTH - 2 * PAD));
                names = ref;
            }
            signatures.add(names);
        }
        return new Footer(List.copyOf(signatures), List.copyOf(notes));
    }
}
