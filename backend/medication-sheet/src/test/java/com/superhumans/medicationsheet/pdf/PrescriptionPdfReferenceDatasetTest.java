package com.superhumans.medicationsheet.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two anonymous datasets under {@code tests/pdf/003-4-o} are the measured print references.
 * Their committed SHA-256 digests are the independent acceptance values produced by
 * {@code tests/pdf/003-4-o/verify.py --original} from the original documents; this test rebuilds
 * the same snapshot graph and pins the real planner/renderer output to those values.
 */
class PrescriptionPdfReferenceDatasetTest {
    private static final Path FIXTURES = fixtures();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void exampleOneReproducesTheReferenceSheets() throws Exception {
        verify(1, 6, "e1d025abc604864440ccf95cbf4c3c02714b39554b1ce5dbc75bc6079017fe5a");
    }

    @Test
    void exampleTwoReproducesTheReferenceSheets() throws Exception {
        verify(2, 8, "7bc345fea985673e3e012dd671376f6f3ebbfdae03296be19200ba4adc308ce9");
    }

    private static void verify(int example, int pages, String digest) throws Exception {
        JsonNode root = JSON.readTree(FIXTURES.resolve("example" + example + ".json").toFile());
        Map<String, PrescriptionPdfSnapshot.Order> orders = new LinkedHashMap<>();
        for (var names = root.get("medicines").fields(); names.hasNext(); ) {
            var entry = names.next();
            orders.put(entry.getKey(), new PrescriptionPdfSnapshot.Order(
                    id("order:" + entry.getKey()), Integer.parseInt(entry.getKey().substring(1)),
                    entry.getValue().asText(), null, null));
        }
        List<Week> weeks = new ArrayList<>();
        for (JsonNode page : root.get("pages")) {
            List<String> rows = new ArrayList<>();
            page.get("rows").forEach(row -> rows.add(row.asText()));
            weeks.add(new Week(LocalDate.parse(page.get("start").asText()), rows,
                    List.of(page.get("nurses").asText().split(" "))));
        }
        Map<UUID, String> logins = new HashMap<>();
        for (Week week : weeks) {
            for (String nurse : week.nurses()) {
                if (!".".equals(nurse)) {
                    logins.put(id("login:" + nurse), nurse);
                }
            }
        }
        List<PrescriptionPdfSnapshot.Row> rows = new ArrayList<>();
        for (int index = 0; index < weeks.size(); index++) {
            Week week = weeks.get(index);
            List<PrescriptionPdfSnapshot.Row> sheet = sheet(week, orders);
            for (int slot = 0; slot < 28; slot++) {
                String nurse = week.nurses().get(slot);
                if (".".equals(nurse)) {
                    continue;
                }
                int position = completedPosition(sheet, week.start().plusDays(slot / 4),
                        PrescriptionPdfPeriod.CODES.get(slot % 4));
                assertThat(position)
                        .as("example %d, week %s, slot %d has a completed fact", example, week.start(), slot)
                        .isGreaterThanOrEqualTo(0);
                var row = sheet.get(position);
                sheet.set(position, new PrescriptionPdfSnapshot.Row(row.order(), row.date(), row.part(),
                        new PrescriptionPdfSnapshot.Execution(id("event:" + index + ":" + slot), row.part().id(),
                                id("login:" + nurse), "Completed", null, null, null, false, null, null)));
            }
            rows.addAll(sheet);
        }
        LocalDate anchor = weeks.stream().map(Week::start).min(LocalDate::compareTo).orElseThrow();
        var snapshot = new PrescriptionPdfSnapshot(anchor, rows);
        var header = new PrescriptionPdfPagePlan.Header("Тестовий заклад", "00123456", null, null,
                false, "Синтетичний пацієнт", "");
        var plans = new PrescriptionPdfPagePlanner().plan(snapshot, header, logins, null, null);

        assertThat(plans).as("example %d physical pages", example).hasSize(pages);
        for (int page = 0; page < plans.size(); page++) {
            var plan = plans.get(page);
            JsonNode expected = root.get("pages").get(page);
            assertThat(plan.week()).as("example %d page %d week", example, page + 1)
                    .isEqualTo(LocalDate.parse(expected.get("start").asText()));
            assertThat(plan.rows()).as("example %d page %d occupied pairs", example, page + 1)
                    .hasSize(expected.get("rows").size());
            long signed = plan.signatures().stream().filter(s -> !s.isEmpty()).count();
            assertThat(signed).as("example %d page %d signed slots", example, page + 1)
                    .isEqualTo(List.of(expected.get("nurses").asText().split(" ")).stream()
                            .filter(nurse -> !".".equals(nurse)).count());
        }
        assertThat(sha256(matrix(plans)))
                .as("example %d planned/completed mark matrix", example)
                .isEqualTo(digest);
        byte[] pdf = new PrescriptionPdfRenderer().render(plans);
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            assertThat(document.getNumberOfPages()).as("example %d rendered pages", example).isEqualTo(pages);
        }
    }

    /** One part per order/date/period; the upper bit is the plan, the lower bit the recorded fact. */
    private static List<PrescriptionPdfSnapshot.Row> sheet(Week week, Map<String, PrescriptionPdfSnapshot.Order> orders) {
        List<PrescriptionPdfSnapshot.Row> rows = new ArrayList<>();
        for (String row : week.rows()) {
            String[] cells = row.split(" ");
            var order = orders.get(cells[0]);
            for (int slot = 0; slot < 28; slot++) {
                boolean planned = cells[1].charAt(slot) == '1';
                boolean completed = cells[2].charAt(slot) == '1';
                if (!planned && !completed) {
                    continue;
                }
                LocalDate date = week.start().plusDays(slot / 4);
                String period = PrescriptionPdfPeriod.CODES.get(slot % 4);
                rows.add(new PrescriptionPdfSnapshot.Row(order, date, new PrescriptionPdfSnapshot.Part(
                        id("part:" + order.id() + ":" + date + ":" + period),
                        id("day:" + order.id() + ":" + date), period, null,
                        planned, false, completed, false, false), null));
            }
        }
        return rows;
    }

    private static int completedPosition(List<PrescriptionPdfSnapshot.Row> rows, LocalDate date, String period) {
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            if (row.event() == null && row.date().equals(date) && row.part().period().equals(period)
                    && row.part().completed()) {
                return i;
            }
        }
        return -1;
    }

    /** The verifier's per-page matrix: 12 pairs x 2 roles x 28 slots, one byte per mark. */
    private static byte[] matrix(List<PrescriptionPdfPagePlan> plans) {
        byte[] bits = new byte[plans.size() * 12 * 2 * 28];
        int position = 0;
        for (var plan : plans) {
            for (int pair = 0; pair < 12; pair++) {
                var owners = pair < plan.rows().size() ? plan.rows().get(pair).block().owners() : Map.<Integer, PrescriptionPdfPrintModel.Cell>of();
                for (int role = 0; role < 2; role++) {
                    for (int slot = 0; slot < 28; slot++) {
                        var cell = owners.get(slot);
                        if (cell != null && (role == 0 ? !cell.plan().isEmpty() : cell.done())) {
                            bits[position] = 1;
                        }
                        position++;
                    }
                }
            }
        }
        return bits;
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static UUID id(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static Path fixtures() {
        for (Path path = Path.of(System.getProperty("user.dir")).toAbsolutePath(); path != null; path = path.getParent()) {
            Path candidate = path.resolve("tests/pdf/003-4-o");
            if (Files.isRegularFile(candidate.resolve("example1.json"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Missing committed reference datasets under tests/pdf/003-4-o");
    }

    private record Week(LocalDate start, List<String> rows, List<String> nurses) { }
}
