package com.superhumans.service;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.superhumans.dto.PdfResponse;
import com.superhumans.entity.*;
import com.superhumans.entity.core.User;
import com.superhumans.entity.TransferStatus;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.repository.core.SystemSettingsRepository;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.repository.icu.ClinicalDayRepository;
import com.superhumans.repository.icu.EpisodeRepository;
import com.superhumans.repository.icu.FluidBalanceRepository;
import com.superhumans.repository.icu.GeneratedPdfRepository;
import com.superhumans.repository.icu.HourlyRecordRepository;
import com.superhumans.repository.icu.MedicalNoteRepository;
import com.superhumans.repository.icu.MedicalOrderRepository;
import com.superhumans.repository.icu.OrderExecutionRepository;
import com.superhumans.repository.icu.ScaleResultRepository;
import com.superhumans.repository.icu.SignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.itextpdf.kernel.colors.ColorConstants;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PdfGeneratorService {

    GeneratedPdfRepository generatedPdfRepository;
    ClinicalDayRepository clinicalDayRepository;
    EpisodeRepository episodeRepository;
    HourlyRecordRepository hourlyRecordRepository;
    AuditService auditService;
    MedicalOrderRepository medicalOrderRepository;
    OrderExecutionRepository orderExecutionRepository;
    MedicalNoteRepository medicalNoteRepository;
    ScaleResultRepository scaleResultRepository;
    FluidBalanceRepository fluidBalanceRepository;
    SignatureRepository signatureRepository;
    UserRepository userRepository;
    MisService misService;
    SystemSettingsRepository systemSettingsRepository;

    public PdfResponse getLatestPdf(UUID clinicalDayId) {
        GeneratedPdf pdf = generatedPdfRepository
                .findFirstByClinicalDayIdOrderByFileVersionDesc(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("PDF не знайдено для клінічного дня: " + clinicalDayId));
        return toResponse(pdf);
    }

    @Transactional
    public PdfResponse generatePdf(UUID clinicalDayId, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Клінічний день не знайдено: " + clinicalDayId));

        if (day.getStatus() == ClinicalDayStatus.OPEN
                || day.getStatus() == ClinicalDayStatus.NURSE_SIGNED) {
            throw new DocumentLockedException("Клінічний день має бути підписаний лікарем перед генерацією PDF");
        }

        int nextVersion = generatedPdfRepository.findFirstByClinicalDayIdOrderByFileVersionDesc(clinicalDayId)
                .map(pdf -> pdf.getFileVersion() + 1)
                .orElse(1);

        byte[] pdfContent = buildPdfContent(day, nextVersion, userId);

        GeneratedPdf pdf = GeneratedPdf.builder()
                .clinicalDay(day)
                .fileName("clinical_day_" + clinicalDayId + "_v" + nextVersion + ".pdf")
                .fileVersion(nextVersion)
                .generatedAt(LocalDateTime.now())
                .generatedBy(userId)
                .build();
        if (pdfContent != null) {
            pdf.setChecksum(Integer.toHexString(pdfContent.hashCode()));
            pdf.setFileData(pdfContent);
        }
        pdf.setTransferStatus(TransferStatus.PENDING);
        pdf.setCreatedBy(userId);
        pdf.setUpdatedBy(userId);
        pdf = generatedPdfRepository.save(pdf);
        auditService.logAction("GeneratedPdf", clinicalDayId, "GENERATE", userId);

        try {
            boolean sent = misService.sendPdf(clinicalDayId, pdfContent, pdf.getFileName(), nextVersion);
            if (sent) {
                pdf.setTransferStatus(TransferStatus.SENT);
                pdf.setTransferredAt(LocalDateTime.now());
            } else {
                pdf.setTransferStatus(TransferStatus.FAILED);
                pdf.setTransferError("MIS відхилив PDF");
            }
        } catch (Exception e) {
            pdf.setTransferStatus(TransferStatus.FAILED);
            pdf.setTransferError(e.getMessage());
        }
        pdf = generatedPdfRepository.save(pdf);
        return toResponse(pdf);
    }

    // ========================================================================
    // PDF CONTENT BUILDING
    // ========================================================================

    private byte[] buildPdfContent(ClinicalDay day, int version, Long userId) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
            Document document = new Document(pdfDoc);

            PdfFont font = loadFont();
            PdfFont boldFont = loadBoldFont(font);

            Episode episode = day.getEpisode();
            List<HourlyRecord> hourlyRecords = hourlyRecordRepository
                    .findByClinicalDayIdOrderByRecordTimeAsc(day.getId());
            List<FluidBalance> balances = fluidBalanceRepository
                    .findByClinicalDayIdOrderByHourAsc(day.getId());
            List<Signature> signatures = signatureRepository.findByClinicalDayId(day.getId());
            List<ScaleResult> scales = scaleResultRepository.findByClinicalDayId(day.getId());
            List<ScaleResult> episodeScales = episode != null
                    ? scaleResultRepository.findByEpisodeId(episode.getId())
                    : Collections.emptyList();
            List<MedicalNote> notes = medicalNoteRepository
                    .findByClinicalDayIdOrderByCreatedAtAsc(day.getId());
            List<MedicalOrder> orders = medicalOrderRepository
                    .findByClinicalDayIdOrderByStartTimeAsc(day.getId());
            PatientDTO patient = null;
            if (episode != null && episode.getPatientId() != null) {
                try {
                    patient = misService.getPatient(episode.getPatientId()).orElse(null);
                } catch (Exception ignored) {}
            }

            int[] displayHours = buildDisplayHours();
            Map<Integer, HourlyRecord> hourMap = buildHourMap(hourlyRecords);

            // Execution hours per order for therapy chart
            Map<UUID, Set<Integer>> orderExecHours = new HashMap<>();
            for (MedicalOrder order : orders) {
                Set<Integer> hours = new HashSet<>();
                List<OrderExecution> execs = orderExecutionRepository.findByOrderId(order.getId());
                for (OrderExecution exec : execs) {
                    if (exec.getExecutedAt() != null) {
                        hours.add(exec.getExecutedAt().getHour());
                    }
                }
                orderExecHours.put(order.getId(), hours);
            }

            // Loss data by display hour index
            double[] urineByHour = new double[24];
            double[] drainByHour = new double[24];
            for (HourlyRecord r : hourlyRecords) {
                int idx = displayHourIndex(r.getRecordHour());
                if (idx >= 0 && idx < 24) {
                    if (r.getUrineOutput() != null) urineByHour[idx] = r.getUrineOutput();
                    if (r.getDrainOutput() != null) drainByHour[idx] = r.getDrainOutput();
                }
            }

            // === MASTER LAYOUT: 2 columns (left content + right sidebar) ===
            float[] masterCols = {86f, 14f};
            Table master = new Table(UnitValue.createPercentArray(masterCols));
            master.setWidth(UnitValue.createPercentValue(100));

            // Header row spanning both columns
            master.addCell(makeHeaderCell(font, boldFont, version, userId, patient, day, episode));
            // Right corner
            master.addCell(cell("").setHeight(26).setBorder(Border.NO_BORDER));

            // Main left content
            Cell leftCell = new Cell().setPadding(0).setBorder(Border.NO_BORDER);
            leftCell.add(createStatsTable(font, boldFont, day, patient, hourMap, displayHours));
            leftCell.add(createTherapyTable(font, boldFont, orders, orderExecHours, displayHours));
            leftCell.add(createLossesTable(font, boldFont, hourMap, displayHours, urineByHour, drainByHour));
            master.addCell(leftCell);

            // Right sidebar
            master.addCell(createSidebar(font, boldFont, patient, episode, balances, signatures, episodeScales, scales));

            document.add(master);

            // Additional pages for notes and scales
            addNotesSection(document, font, boldFont, notes);
            List<ScaleResult> allScales = new ArrayList<>(scales);
            allScales.addAll(episodeScales);
            addScalesSection(document, font, boldFont, allScales);
            addFooter(document, font, version, userId);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    // ========================================================================
    // DISPLAY HOUR HELPERS
    // ========================================================================

    private int[] buildDisplayHours() {
        int[] h = new int[24];
        for (int i = 0; i < 16; i++) h[i] = i + 8;
        h[16] = 24;
        for (int i = 17; i < 24; i++) h[i] = i - 16;
        return h;
    }

    private int displayHourIndex(int recordHour) {
        return (recordHour >= 8) ? (recordHour - 8) : (recordHour + 16);
    }

    private Map<Integer, HourlyRecord> buildHourMap(List<HourlyRecord> records) {
        Map<Integer, HourlyRecord> map = new HashMap<>();
        for (HourlyRecord r : records) {
            map.put(displayHourIndex(r.getRecordHour()), r);
        }
        return map;
    }

    // ========================================================================
    // FORM HEADER CELL
    // ========================================================================

    private Cell makeHeaderCell(PdfFont font, PdfFont boldFont, int version, Long userId,
                                 PatientDTO patient, ClinicalDay day, Episode episode) {
        String institution = loadInstitutionName();
        Table inner = new Table(new float[]{48f, 14f, 38f});
        inner.setWidth(UnitValue.createPercentValue(100));

        String minName = "Найменування міністерства, іншого органу виконавчої влади,\n"
                + "підприємства, установи, організації, до сфери управління якого\n"
                + "належить заклад охорони здоров'я";
        String addrName = "Найменування та місцезнаходження (повна поштова адреса)\n"
                + "закладу охорони здоров'я, де заповнена форма";
        String edrpou = institution.contains("ЄДРПОУ") ? institution
                : (institution.isEmpty() ? "" : institution + "\nКод за ЄДРПОУ 1234567890");

        Cell leftCell = cell(minName + "\n" + addrName + "\n" + edrpou).setFontSize(8).setPadding(3);
        Cell centerCell = cell("").setBorder(Border.NO_BORDER);
        String headerRight = "МЕДИЧНА ДОКУМЕНТАЦІЯ\n"
                + "Форма первинної облікової документації № 003-15/о\n"
                + "ЗАТВЕРДЖЕНО\n"
                + "Наказ МОЗ України\n"
                + (version > 0 ? String.valueOf(version) : "123456789");
        Cell rightCell = cell(headerRight).setFontSize(9).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(3);

        inner.addCell(leftCell.setFont(font));
        inner.addCell(centerCell);
        inner.addCell(rightCell.setFont(font));

        // Second row: title
        Cell titleRow = cell("КАРТА ІНТЕНСИВНОЇ ТЕРАПІЇ")
                .setFont(boldFont).setFontSize(14)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setPadding(4);

        Table wrapper = new Table(1);
        wrapper.setWidth(UnitValue.createPercentValue(100));
        wrapper.addCell(new Cell().add(inner).setBorder(Border.NO_BORDER).setPadding(0));
        wrapper.addCell(new Cell().add(titleRow).setBorder(Border.NO_BORDER).setPadding(0));

        Cell master = new Cell().add(wrapper).setPadding(0);
        return master;
    }

    // ========================================================================
    // MEDICAL STATS TABLE  (24h × 8 metrics)
    // ========================================================================

    private Table createStatsTable(PdfFont font, PdfFont boldFont, ClinicalDay day,
                                    PatientDTO patient, Map<Integer, HourlyRecord> hourMap, int[] displayHours) {
        int[] dh = displayHours;
        int totalCols = 3 + 24;
        float[] colWidths = new float[totalCols];
        colWidths[0] = 14f;
        colWidths[1] = 1.6f;
        colWidths[2] = 8.4f;
        for (int i = 3; i < totalCols; i++) colWidths[i] = 76f / 24f;

        Table t = new Table(UnitValue.createPercentArray(colWidths));
        t.setWidth(UnitValue.createPercentValue(100));

        float dataSize = 6f;
        float headerSize = 7f;

        // Row 0: info + hour headers
        t.addCell(infoCell("Дата заповнення:", font, boldFont, headerSize));
        t.addCell(cell("").setFontSize(headerSize));
        t.addCell(headerCell("Час", font, boldFont, headerSize));
        for (int i = 0; i < 24; i++) {
            t.addCell(headerCell(String.valueOf(dh[i]), font, boldFont, headerSize));
        }

        // Metrics rows
        String[] metrics = {
                "АТсист (мм.рт.ст)", "АТдіас (мм.рт.ст)", "ЧСС ( в 1 хв)",
                "SpO₂ (%)", "Темп. тіла (°С)", "ЦВТ (мм.вод.ст)", "Дихання", "ЧД (в 1 хв.)"
        };
        String[] infoLabels = {"Вік пацієнта:", "Доба перебування у ВАІТ:"};
        String[] infoValues = new String[2];
        infoValues[0] = patient != null && patient.getBirthDate() != null
                ? ChronoUnit.YEARS.between(patient.getBirthDate(), LocalDateTime.now()) + " р."
                : "";
        infoValues[1] = String.valueOf(day.getDayNumber());

        // Row 1: info1 + vertical label + metric0
        t.addCell(infoCell(infoLabels[0], font, boldFont, headerSize));
        t.addCell(verticalCell("Показники", font, boldFont, headerSize, 8));
        t.addCell(metricCell(metrics[0], font, headerSize));
        for (int i = 0; i < 24; i++) {
            HourlyRecord r = hourMap.get(i);
            t.addCell(dataCell(formatStat(r, 0), font, dataSize));
        }

        // Row 2: info2 (rowspan=7) + metric1
        t.addCell(new Cell(7, 1)
                .add(new Paragraph(infoLabels[1]).setFont(font).setBold().setFontSize(headerSize))
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .setPadding(2));
        t.addCell(metricCell(metrics[1], font, headerSize));
        for (int i = 0; i < 24; i++) {
            HourlyRecord r = hourMap.get(i);
            t.addCell(dataCell(formatStat(r, 1), font, dataSize));
        }

        // Rows 3-8: metrics[2..7]
        for (int m = 2; m < metrics.length; m++) {
            t.addCell(metricCell(metrics[m], font, headerSize));
            int mi = m;
            for (int i = 0; i < 24; i++) {
                HourlyRecord r = hourMap.get(i);
                t.addCell(dataCell(formatStat(r, mi), font, dataSize));
            }
        }

        return t;
    }

    private String formatStat(HourlyRecord r, int idx) {
        if (r == null) return "";
        return switch (idx) {
            case 0 -> r.getSystolicBP() != null ? String.valueOf(r.getSystolicBP().intValue()) : "";
            case 1 -> r.getDiastolicBP() != null ? String.valueOf(r.getDiastolicBP().intValue()) : "";
            case 2 -> r.getHeartRate() != null ? String.valueOf(r.getHeartRate().intValue()) : "";
            case 3 -> r.getSpo2() != null ? String.format("%.0f", r.getSpo2()) : "";
            case 4 -> r.getTemperature() != null ? String.format("%.1f", r.getTemperature()) : "";
            case 5 -> r.getCvp() != null ? String.valueOf(r.getCvp().intValue()) : "";
            case 6 -> r.getConsciousness() != null ? r.getConsciousness() : "";
            case 7 -> r.getRespiratoryRate() != null ? String.valueOf(r.getRespiratoryRate().intValue()) : "";
            default -> "";
        };
    }

    // ========================================================================
    // THERAPY TABLE  (9 rows × 24h)
    // ========================================================================

    private Table createTherapyTable(PdfFont font, PdfFont boldFont, List<MedicalOrder> orders,
                                      Map<UUID, Set<Integer>> execHours, int[] displayHours) {
        float[] cw = new float[3 + 24];
        cw[0] = 14f; cw[1] = 2f; cw[2] = 8f;
        for (int i = 3; i < cw.length; i++) cw[i] = 76f / 24f;

        Table t = new Table(UnitValue.createPercentArray(cw));
        t.setWidth(UnitValue.createPercentValue(100));

        float cellSize = 6f;
        float headerSize = 8f;

        // Title row
        t.addCell(new Cell(1, 3).add(new Paragraph("Терапія").setFont(boldFont).setFontSize(headerSize))
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(2));
        for (int i = 0; i < 24; i++) {
            t.addCell(cell("").setFontSize(cellSize));
        }

        // 9 data rows
        int orderCount = Math.min(orders.size(), 9);
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < orderCount; i++) {
            MedicalOrder o = orders.get(i);
            labels.add(o.getDrugName() + " " + (o.getDose() != null ? o.getDose() : "") + (o.getUnit() != null ? " " + o.getUnit() : ""));
        }
        while (labels.size() < 9) labels.add("");

        for (int row = 0; row < 9; row++) {
            boolean groupStart = (row == 3 || row == 6);
            float borderW = groupStart ? 1.5f : 0.5f;

            t.addCell(cell("").setFontSize(cellSize));
            t.addCell(cell("").setFontSize(cellSize));
            t.addCell(metricCell(labels.get(row), font, cellSize));

            String label = labels.get(row);
            for (int i = 0; i < 24; i++) {
                String mark = "";
                if (row < orderCount && !label.isEmpty()) {
                    MedicalOrder o = orders.get(row);
                    Set<Integer> hours = execHours.getOrDefault(o.getId(), Collections.emptySet());
                    if (hours.contains(displayHours[i])) mark = "+";
                }
                t.addCell(dataCell(mark, font, cellSize));
            }
        }

        return t;
    }

    // ========================================================================
    // LOSSES TABLE  (4 rows × 24h)
    // ========================================================================

    private Table createLossesTable(PdfFont font, PdfFont boldFont, Map<Integer, HourlyRecord> hourMap,
                                     int[] displayHours, double[] urineByHour, double[] drainByHour) {
        float[] cw = new float[1 + 24];
        cw[0] = 24f;
        for (int i = 1; i < cw.length; i++) cw[i] = 76f / 24f;

        Table t = new Table(UnitValue.createPercentArray(cw));
        t.setWidth(UnitValue.createPercentValue(100));

        float cellSize = 6f;
        float headerSize = 8f;

        // Title row spanning all 25 columns
        t.addCell(new Cell(1, 25)
                .add(new Paragraph("Втрати").setFont(boldFont).setFontSize(headerSize))
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(2));

        // 4 loss rows: urine, drain, stool, vomit
        String[] lossLabels = {"Діурез (сеча)", "Дренаж", "Стілець", "Блювота"};
        for (int row = 0; row < 4; row++) {
            t.addCell(metricCell(lossLabels[row], font, cellSize));
            for (int i = 0; i < 24; i++) {
                HourlyRecord r = hourMap.get(i);
                String val = switch (row) {
                    case 0 -> urineByHour[i] > 0 ? String.format("%.0f", urineByHour[i]) : "";
                    case 1 -> drainByHour[i] > 0 ? String.format("%.0f", drainByHour[i]) : "";
                    case 2 -> r != null && r.getStool() != null ? r.getStool().toString() : "";
                    case 3 -> r != null && r.getVomit() != null ? r.getVomit().toString() : "";
                    default -> "";
                };
                t.addCell(dataCell(val, font, cellSize));
            }
        }

        return t;
    }

    // ========================================================================
    // SIDEBAR
    // ========================================================================

    private Cell createSidebar(PdfFont font, PdfFont boldFont, PatientDTO patient, Episode episode,
                                List<FluidBalance> balances, List<Signature> signatures,
                                List<ScaleResult> episodeScales, List<ScaleResult> scales) {
        float cellSize = 7f;
        float titleSize = 8f;

        Table sb = new Table(1);
        sb.setWidth(UnitValue.createPercentValue(100));

        // Patient name
        String name = patient != null ? patient.getFullName() : "Пацієнт (ПІП)";
        sb.addCell(sideCell(name, font, cellSize).setMinHeight(40));

        // Diagnosis
        sb.addCell(sideCell("Діагноз:", font, cellSize).setMinHeight(50));

        // Anthropometry
        sb.addCell(sideCell("Маса тіла: ________________", font, cellSize).setMinHeight(12));
        sb.addCell(sideCell("Зріст: ____________________", font, cellSize).setMinHeight(12));
        sb.addCell(sideCell("Ідеальна маса тіла: ________", font, cellSize).setMinHeight(12));

        // Blood group + Rh
        String bloodGroup = patient != null && patient.getBloodGroup() != null ? patient.getBloodGroup() : "____";
        String rhFactor = patient != null && patient.getRhFactor() != null ? patient.getRhFactor() : "____";
        sb.addCell(sideCell("Група крові: " + bloodGroup, font, cellSize).setMinHeight(12));
        sb.addCell(sideCell("Резус-фактор: " + rhFactor, font, cellSize).setMinHeight(12));

        // APACHE II
        String apacheVal = findScaleValue(episodeScales, "APACHE II");
        sb.addCell(sideCell("Важкість стану за APACHE II: " + (apacheVal != null ? apacheVal : "____"), font, cellSize).setMinHeight(12));

        // SOFA
        String sofaVal = findScaleValue(scales, "SOFA");
        sb.addCell(sideCell("Ступінь тяж. ПОН за SOFA: " + (sofaVal != null ? sofaVal : "____"), font, cellSize).setMinHeight(12));

        // Fluid balance
        sb.addCell(sideSectionTitle("Баланс рідини", font, boldFont, titleSize));
        double totalIntake = 0, totalOutput = 0;
        for (FluidBalance fb : balances) {
            if (fb.getIntake() != null) totalIntake += fb.getIntake();
            if (fb.getOutput() != null) totalOutput += fb.getOutput();
        }
        double netBalance = totalIntake - totalOutput;
        sb.addCell(sideCell("Поступлення: " + fmt(totalIntake), font, cellSize).setMinHeight(10));
        sb.addCell(sideCell("Виділення: " + fmt(totalOutput), font, cellSize).setMinHeight(10));
        sb.addCell(sideCell("Добовий баланс: " + fmtSigned(netBalance), font, cellSize).setMinHeight(10));

        double cumulative = 0;
        if (!balances.isEmpty()) {
            FluidBalance last = balances.get(balances.size() - 1);
            if (last.getCumulativeBalance() != null) cumulative = last.getCumulativeBalance();
        }
        sb.addCell(sideCell("Кумулятивний баланс: " + fmtSigned(cumulative), font, cellSize).setMinHeight(10));

        // Nurse signatures
        sb.addCell(sideSectionTitle("Сестри/брати медичні\n(Власне ім'я, прізвище та підпис)", font, boldFont, titleSize));
        List<Signature> nurseSigs = signatures.stream()
                .filter(s -> "NURSE".equals(s.getRole()) && "ACTIVE".equals(s.getStatus()))
                .collect(Collectors.toList());
        int ns = Math.min(nurseSigs.size(), 2);
        for (int i = 0; i < 2; i++) {
            String line = (i + 1) + ". ";
            if (i < ns) {
                line += lookupUserName(nurseSigs.get(i).getUserId());
            }
            sb.addCell(sideCell(line, font, cellSize).setMinHeight(14));
        }

        // Doctor signatures
        sb.addCell(sideSectionTitle("Лікарі\n(Власне ім'я, прізвище та підпис)", font, boldFont, titleSize));
        List<Signature> docSigs = signatures.stream()
                .filter(s -> ("DOCTOR".equals(s.getRole()) || "HEAD_OF_DEPARTMENT".equals(s.getRole())) && "ACTIVE".equals(s.getStatus()))
                .collect(Collectors.toList());
        int ds = Math.min(docSigs.size(), 2);
        for (int i = 0; i < 2; i++) {
            String line = (i + 1) + ". ";
            if (i < ds) {
                line += lookupUserName(docSigs.get(i).getUserId());
            }
            sb.addCell(sideCell(line, font, cellSize).setMinHeight(14));
        }

        Cell wrapper = new Cell().add(sb).setPadding(0);
        return wrapper;
    }

    // ========================================================================
    // ADDITIONAL SECTIONS (after main form)
    // ========================================================================

    private void addNotesSection(Document doc, PdfFont font, PdfFont boldFont, List<MedicalNote> notes) {
        if (notes.isEmpty()) return;
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("МЕДИЧНІ НОТАТКИ")
                .setFont(boldFont).setFontSize(12).setUnderline());

        Map<Long, String> authorNames = getAuthorNames(notes);
        for (MedicalNote note : notes) {
            String author = authorNames.getOrDefault(note.getAuthorId(),
                    String.valueOf(note.getAuthorId()));
            doc.add(new Paragraph("  [" + note.getNoteType() + "] " + author + " ("
                    + translateRole(note.getRole()) + "):")
                    .setFont(font).setBold());
            doc.add(new Paragraph("    " + note.getText()).setFont(font));
            doc.add(new Paragraph("    — " + note.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).setFont(font));
            doc.add(new Paragraph(" "));
        }
    }

    private void addScalesSection(Document doc, PdfFont font, PdfFont boldFont, List<ScaleResult> scales) {
        if (scales.isEmpty()) return;
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("КЛІНІЧНІ ШКАЛИ")
                .setFont(boldFont).setFontSize(12).setUnderline());

        Table table = new Table(UnitValue.createPercentArray(new float[]{30, 20, 25, 25}))
                .useAllAvailableWidth();
        addTableHeader(table, font, "Шкала", "Результат", "Обчислено", "Ким");
        Map<Long, String> userNames = getScaleUserNames(scales);
        for (ScaleResult sr : scales) {
            addTableRow(table, font,
                    sr.getScale().getName(),
                    sr.getResult(),
                    sr.getCalculatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    userNames.getOrDefault(sr.getCalculatedBy(),
                            String.valueOf(sr.getCalculatedBy())));
        }
        doc.add(table);
    }

    private void addFooter(Document doc, PdfFont font, int version, Long userId) {
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("— Кінець документа —")
                .setFont(font).setFontSize(10).setHorizontalAlignment(HorizontalAlignment.CENTER));
        doc.add(new Paragraph("Документ v" + version + " | Сформовано користувачем: " + userId
                + " | " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))
                .setFont(font).setFontSize(8).setHorizontalAlignment(HorizontalAlignment.CENTER));
    }

    // ========================================================================
    // CELL HELPERS
    // ========================================================================

    private Cell cell(String text) {
        Paragraph p = new Paragraph(text != null ? text : "");
        return new Cell().add(p).setPadding(1);
    }

    private Cell infoCell(String text, PdfFont font, PdfFont boldFont, float size) {
        return cell(text).setFont(font).setFontSize(size).setBold().setVerticalAlignment(
                com.itextpdf.layout.properties.VerticalAlignment.TOP).setPadding(2);
    }

    private Cell headerCell(String text, PdfFont font, PdfFont boldFont, float size) {
        return cell(text).setFont(font).setFontSize(size).setBold()
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
    }

    private Cell metricCell(String text, PdfFont font, float size) {
        return cell(text).setFont(font).setFontSize(size).setPadding(2);
    }

    private Cell dataCell(String text, PdfFont font, float size) {
        return cell(text).setFont(font).setFontSize(size)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
    }

    private Cell verticalCell(String text, PdfFont font, PdfFont boldFont, float size, int rowspan) {
        Paragraph p = new Paragraph(text).setFont(boldFont).setFontSize(size)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        return new Cell(rowspan, 1).add(p).setVerticalAlignment(
                com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
    }

    private Cell sideCell(String text, PdfFont font, float size) {
        return cell(text).setFont(font).setFontSize(size).setPadding(2);
    }

    private Cell sideSectionTitle(String text, PdfFont font, PdfFont boldFont, float size) {
        return cell(text).setFont(boldFont).setFontSize(size).setPadding(2);
    }

    // ========================================================================
    // ORIGINAL UTILITY METHODS (kept)
    // ========================================================================

    private String loadInstitutionName() {
        try {
            return systemSettingsRepository.findByKey("institution_name")
                    .map(s -> s.getValue())
                    .orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    private Paragraph keyValue(String key, String value, PdfFont font) {
        return new Paragraph("  " + key + ": " + value).setFont(font).setFontSize(10);
    }

    private void addTableHeader(Table table, PdfFont font, String... headers) {
        for (String header : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(header).setFont(font).setFontSize(7).setBold()));
        }
    }

    private void addTableRow(Table table, PdfFont font, String... values) {
        for (String value : values) {
            table.addCell(new Cell().add(new Paragraph(value).setFont(font).setFontSize(6)));
        }
    }

    private String val(Object o) {
        return o != null ? o.toString() : "";
    }

    private String pct(Object o) {
        if (o == null) return "";
        if (o instanceof Double d) return String.format("%.1f%%", d);
        return o + "%";
    }

    private String fmt(Double d) {
        if (d == null) return "0.0";
        return String.format("%.1f", d);
    }

    private String fmtSigned(Double d) {
        if (d == null) return "0.0";
        return (d >= 0 ? "+" : "") + String.format("%.1f", d);
    }

    private String translateStatus(ClinicalDayStatus status) {
        return switch (status) {
            case OPEN -> "ВІДКРИТИЙ";
            case NURSE_SIGNED -> "ПІДПИСАНО МЕДСЕСТРОЮ";
            case DOCTOR_SIGNED -> "ПІДПИСАНО ЛІКАРЕМ";
            case CLOSED -> "ЗАКРИТО";
            case REOPENED -> "ВІДКРИТО ЗАНОВО";
        };
    }

    private String translateEpisodeStatus(EpisodeStatus status) {
        return switch (status) {
            case DRAFT -> "ЧЕРНЕТКА";
            case ACTIVE -> "АКТИВНИЙ";
            case COMPLETED -> "ЗАВЕРШЕНО";
            case ARCHIVED -> "АРХІВОВАНО";
        };
    }

    private String translateSex(String code) {
        if (code == null) return "Н/Д";
        return "M".equals(code) ? "Чоловіча" : "Жіноча";
    }

    private String translateRole(String role) {
        if (role == null) return "";
        return switch (role) {
            case "DOCTOR" -> "Лікар";
            case "NURSE" -> "Медична сестра";
            case "HEAD_OF_DEPARTMENT" -> "Завідувач відділення";
            case "ADMINISTRATOR" -> "Адміністратор";
            case "AUDITOR" -> "Аудитор";
            default -> role;
        };
    }

    private String translateOrderStatus(MedicalOrderStatus status) {
        return switch (status) {
            case DRAFT -> "ЧЕРНЕТКА";
            case ACTIVE -> "АКТИВНЕ";
            case COMPLETED -> "ВИКОНАНО";
            case CANCELLED -> "СКАСОВАНО";
        };
    }

    private String translateExecutionStatus(OrderExecutionStatus status) {
        return switch (status) {
            case PLANNED -> "ЗАПЛАНОВАНО";
            case IN_PROGRESS -> "ВИКОНУЄТЬСЯ";
            case COMPLETED -> "ВИКОНАНО";
            case PARTIALLY_COMPLETED -> "ЧАСТКОВО ВИКОНАНО";
            case CANCELLED -> "СКАСОВАНО";
        };
    }

    private String translateSignatureStatus(String status) {
        if (status == null) return "";
        return switch (status) {
            case "ACTIVE" -> "АКТИВНИЙ";
            case "REVOKED" -> "ВІДКЛИКАНО";
            default -> status;
        };
    }

    private Map<Long, String> getUserNames(List<OrderExecution> executions) {
        Map<Long, String> names = new HashMap<>();
        for (OrderExecution exec : executions) {
            if (!names.containsKey(exec.getExecutedBy())) {
                names.put(exec.getExecutedBy(), lookupUserName(exec.getExecutedBy()));
            }
        }
        return names;
    }

    private Map<Long, String> getAuthorNames(List<MedicalNote> notes) {
        Map<Long, String> names = new HashMap<>();
        for (MedicalNote note : notes) {
            if (!names.containsKey(note.getAuthorId())) {
                names.put(note.getAuthorId(), lookupUserName(note.getAuthorId()));
            }
        }
        return names;
    }

    private Map<Long, String> getScaleUserNames(List<ScaleResult> scales) {
        Map<Long, String> names = new HashMap<>();
        for (ScaleResult sr : scales) {
            if (!names.containsKey(sr.getCalculatedBy())) {
                names.put(sr.getCalculatedBy(), lookupUserName(sr.getCalculatedBy()));
            }
        }
        return names;
    }

    private Map<Long, String> getSignatureUserNames(List<Signature> signatures) {
        Map<Long, String> names = new HashMap<>();
        for (Signature sig : signatures) {
            if (!names.containsKey(sig.getUserId())) {
                names.put(sig.getUserId(), lookupUserName(sig.getUserId()));
            }
        }
        return names;
    }

    private String lookupUserName(Long userId) {
        try {
            return userRepository.findById(userId)
                    .map(User::getFullName)
                    .orElse(userId.toString().substring(0, 8));
        } catch (Exception e) {
            return userId.toString().substring(0, 8);
        }
    }

    private String findScaleValue(List<ScaleResult> scales, String name) {
        for (ScaleResult sr : scales) {
            if (sr.getScale() != null && name.equalsIgnoreCase(sr.getScale().getName())) {
                return sr.getResult();
            }
        }
        return null;
    }

    private PdfFont loadFont() {
        String[] fontPaths = {
                "fonts/DejaVuSans.ttf",
                "C:/Windows/Fonts/arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/msttcorefonts/Arial.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
        };
        for (String path : fontPaths) {
            try {
                return PdfFontFactory.createFont(path);
            } catch (Exception ignored) {}
        }
        try {
            return PdfFontFactory.createFont("Helvetica");
        } catch (Exception e) {
            return null;
        }
    }

    private PdfFont loadBoldFont(PdfFont regular) {
        if (regular == null) return null;
        String name = regular.getFontProgram().getFontNames().getFontName();
        if (name != null && !name.isEmpty()) {
            String boldPath = name.replace("Sans-Regular", "Sans-Bold")
                    .replace("arial", "arialbd")
                    .replace("Arial", "Arial-Bold");
            try {
                return PdfFontFactory.createFont(boldPath);
            } catch (Exception ignored) {}
        }
        String[] boldPaths = {
                "fonts/DejaVuSans-Bold.ttf",
                "C:/Windows/Fonts/arialbd.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                "/usr/share/fonts/truetype/msttcorefonts/Arial-Bold.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
        };
        for (String path : boldPaths) {
            try {
                return PdfFontFactory.createFont(path);
            } catch (Exception ignored) {}
        }
        return regular;
    }

    private static PdfResponse toResponse(GeneratedPdf entity) {
        return PdfResponse.builder()
                .id(entity.getId())
                .clinicalDayId(entity.getClinicalDay().getId())
                .fileName(entity.getFileName())
                .fileVersion(entity.getFileVersion())
                .generatedAt(entity.getGeneratedAt())
                .generatedBy(entity.getGeneratedBy())
                .checksum(entity.getChecksum())
                .transferStatus(entity.getTransferStatus())
                .transferredAt(entity.getTransferredAt())
                .transferError(entity.getTransferError())
                .build();
    }
}
