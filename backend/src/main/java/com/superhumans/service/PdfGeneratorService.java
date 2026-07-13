package com.superhumans.service;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.superhumans.dto.PdfResponse;
import com.superhumans.entity.*;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private final GeneratedPdfRepository generatedPdfRepository;
    private final ClinicalDayRepository clinicalDayRepository;
    private final EpisodeRepository episodeRepository;
    private final HourlyRecordRepository hourlyRecordRepository;
    private final AuditService auditService;
    private final MedicalOrderRepository medicalOrderRepository;
    private final OrderExecutionRepository orderExecutionRepository;
    private final MedicalNoteRepository medicalNoteRepository;
    private final ScaleResultRepository scaleResultRepository;
    private final FluidBalanceRepository fluidBalanceRepository;
    private final SignatureRepository signatureRepository;
    private final UserRepository userRepository;
    private final MisService misService;

    public PdfResponse getLatestPdf(UUID clinicalDayId) {
        GeneratedPdf pdf = generatedPdfRepository
                .findFirstByClinicalDayIdOrderByFileVersionDesc(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("No PDF found for clinical day: " + clinicalDayId));
        return toResponse(pdf);
    }

    @Transactional
    public PdfResponse generatePdf(UUID clinicalDayId, UUID userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + clinicalDayId));

        if (day.getStatus() == ClinicalDayStatus.OPEN
                || day.getStatus() == ClinicalDayStatus.NURSE_SIGNED) {
            throw new DocumentLockedException("Clinical day must be signed by doctor before generating PDF");
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
        }
        pdf.setCreatedBy(userId);
        pdf.setUpdatedBy(userId);
        pdf = generatedPdfRepository.save(pdf);
        auditService.logAction("GeneratedPdf", clinicalDayId, "GENERATE", userId);
        return toResponse(pdf);
    }

    private byte[] buildPdfContent(ClinicalDay day, int version, UUID userId) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont font = loadFont();
            PdfFont boldFont = loadBoldFont(font);

            addHeader(document, font, boldFont, version, userId);
            addSection(document, font, boldFont, day);
            addEpisodeInfo(document, font, boldFont, day);
            addHourlyRecords(document, font, boldFont, day);
            addFluidBalance(document, font, boldFont, day);
            addMedicalOrders(document, font, boldFont, day);
            addMedicalNotes(document, font, boldFont, day);
            addScaleResults(document, font, boldFont, day);
            addSignatures(document, font, boldFont, day);
            addFooter(document, font, version, userId);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private void addHeader(Document doc, PdfFont font, PdfFont boldFont, int version, UUID userId) {
        doc.add(new Paragraph("ICU PATIENT CHART")
                .setFont(boldFont).setFontSize(18).setHorizontalAlignment(HorizontalAlignment.CENTER));
        doc.add(new Paragraph("Clinical Day Report")
                .setFont(font).setFontSize(14).setHorizontalAlignment(HorizontalAlignment.CENTER));
        doc.add(new Paragraph("Version: " + version + " | Generated: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .setFont(font).setFontSize(10).setHorizontalAlignment(HorizontalAlignment.CENTER));
        doc.add(new Paragraph(" "));
    }

    private void addSection(Document doc, PdfFont font, PdfFont boldFont, ClinicalDay day) {
        doc.add(new Paragraph("CLINICAL DAY INFORMATION")
                .setFont(boldFont).setFontSize(13).setUnderline());
        doc.add(keyValue("Day Number", String.valueOf(day.getDayNumber()), font));
        doc.add(keyValue("Status", day.getStatus().name(), font));
        doc.add(keyValue("Period", day.getStartDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + " - " + (day.getEndDateTime() != null
                ? day.getEndDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A"), font));
        if (day.getClosedAt() != null) {
            doc.add(keyValue("Closed At", day.getClosedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), font));
        }
        doc.add(new Paragraph(" "));
    }

    private void addEpisodeInfo(Document doc, PdfFont font, PdfFont boldFont, ClinicalDay day) {
        doc.add(new Paragraph("EPISODE INFORMATION")
                .setFont(boldFont).setFontSize(13).setUnderline());

        if (day.getEpisode() != null) {
            Episode episode = day.getEpisode();
            doc.add(keyValue("Episode ID", episode.getId().toString(), font));
            doc.add(keyValue("Admission", episode.getAdmissionDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), font));
            doc.add(keyValue("Status", episode.getStatus().name(), font));

            if (episode.getPatientId() != null) {
                try {
                    PatientDTO patient = misService.getPatient(episode.getPatientId()).orElse(null);
                    if (patient != null) {
                        doc.add(keyValue("Patient", patient.getFullName(), font));
                        doc.add(keyValue("Date of Birth", patient.getBirthDate() != null
                                ? patient.getBirthDate().toString() : "N/A", font));
                        doc.add(keyValue("Sex", patient.getSexCode() != null
                                ? ("M".equals(patient.getSexCode()) ? "Male" : "Female") : "N/A", font));
                        if (patient.getExternalId1() != null)
                            doc.add(keyValue("MRN", patient.getExternalId1(), font));
                    }
                } catch (Exception ignored) {
                    doc.add(keyValue("Patient ID", episode.getPatientId().toString(), font));
                }
            }
        }
        doc.add(new Paragraph(" "));
    }

    private void addHourlyRecords(Document doc, PdfFont font, PdfFont boldFont, ClinicalDay day) {
        doc.add(new Paragraph("HOURLY RECORDS")
                .setFont(boldFont).setFontSize(13).setUnderline());

        List<HourlyRecord> records = hourlyRecordRepository
                .findByClinicalDayIdOrderByRecordTimeAsc(day.getId());
        if (records.isEmpty()) {
            doc.add(new Paragraph("  No records.").setFont(font));
        } else {
            Table table = new Table(UnitValue.createPercentArray(new float[]{14, 10, 8, 8, 8, 8, 8, 8, 8, 10}))
                    .useAllAvailableWidth();
            addTableHeader(table, font, "Time", "HR", "RR", "BP Sys", "BP Dia", "SpO2", "Temp", "Pain", "Urine", "CSP");
            for (HourlyRecord r : records) {
                addTableRow(table, font,
                        r.getRecordTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        val(r.getHeartRate()), val(r.getRespiratoryRate()),
                        val(r.getSystolicBP()), val(r.getDiastolicBP()),
                        pct(r.getSpo2()), val(r.getTemperature()), val(r.getPainScore()),
                        val(r.getUrineOutput()), r.getConsciousness() != null ? r.getConsciousness() : "");
            }
            doc.add(table);
        }
        doc.add(new Paragraph(" "));
    }

    private void addFluidBalance(Document doc, PdfFont font, PdfFont boldFont, ClinicalDay day) {
        doc.add(new Paragraph("FLUID BALANCE")
                .setFont(boldFont).setFontSize(13).setUnderline());

        List<FluidBalance> balances = fluidBalanceRepository
                .findByClinicalDayIdOrderByHourAsc(day.getId());
        if (balances.isEmpty()) {
            doc.add(new Paragraph("  Not calculated. Use POST /fluid-balance/recalculate to compute.")
                    .setFont(font));
        } else {
            Table table = new Table(UnitValue.createPercentArray(new float[]{12, 22, 22, 22, 22}))
                    .useAllAvailableWidth();
            addTableHeader(table, font, "Hour", "Intake", "Output", "Balance", "Cumulative");
            for (FluidBalance fb : balances) {
                addTableRow(table, font,
                        fb.getHour() + ":00",
                        fmt(fb.getIntake()), fmt(fb.getOutput()),
                        fmtSigned(fb.getBalance()), fmtSigned(fb.getCumulativeBalance()));
            }
            doc.add(table);
        }
        doc.add(new Paragraph(" "));
    }

    private void addMedicalOrders(Document doc, PdfFont font, PdfFont boldFont, ClinicalDay day) {
        doc.add(new Paragraph("MEDICAL ORDERS")
                .setFont(boldFont).setFontSize(13).setUnderline());

        List<MedicalOrder> orders = medicalOrderRepository
                .findByClinicalDayIdOrderByStartTimeAsc(day.getId());
        if (orders.isEmpty()) {
            doc.add(new Paragraph("  No orders.").setFont(font));
        } else {
            for (MedicalOrder order : orders) {
                doc.add(new Paragraph("  " + order.getDrugName() + " | " + order.getDose() + " " + order.getUnit()
                        + " | " + order.getRoute() + " | " + order.getFrequency()
                        + " | Status: " + order.getStatus())
                        .setFont(font));

                List<OrderExecution> executions = orderExecutionRepository.findByOrderId(order.getId());
                if (!executions.isEmpty()) {
                    Table execTable = new Table(UnitValue.createPercentArray(new float[]{16, 20, 20, 20, 24}))
                            .useAllAvailableWidth();
                    addTableHeader(execTable, font, "Executed At", "By", "Dose", "Status", "Comment");
                    Map<UUID, String> userNames = getUserNames(executions);
                    for (OrderExecution exec : executions) {
                        addTableRow(execTable, font,
                                exec.getExecutedAt().format(DateTimeFormatter.ofPattern("HH:mm")),
                                userNames.getOrDefault(exec.getExecutedBy(), exec.getExecutedBy().toString().substring(0, 8)),
                                exec.getActualDose() != null ? exec.getActualDose() : "",
                                exec.getStatus().name(),
                                exec.getComment() != null ? exec.getComment() : "");
                    }
                    doc.add(execTable);
                }
                doc.add(new Paragraph(" "));
            }
        }
        doc.add(new Paragraph(" "));
    }

    private void addMedicalNotes(Document doc, PdfFont font, PdfFont boldFont, ClinicalDay day) {
        doc.add(new Paragraph("MEDICAL NOTES")
                .setFont(boldFont).setFontSize(13).setUnderline());

        List<MedicalNote> notes = medicalNoteRepository
                .findByClinicalDayIdOrderByCreatedAtAsc(day.getId());
        if (notes.isEmpty()) {
            doc.add(new Paragraph("  No notes.").setFont(font));
        } else {
            Map<UUID, String> authorNames = getAuthorNames(notes);
            for (MedicalNote note : notes) {
                String author = authorNames.getOrDefault(note.getAuthorId(),
                        note.getAuthorId().toString().substring(0, 8));
                doc.add(new Paragraph("  [" + note.getNoteType() + "] " + author + " (" + note.getRole() + "):")
                        .setFont(font).setBold());
                doc.add(new Paragraph("    " + note.getText()).setFont(font));
                doc.add(new Paragraph("    — " + note.getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).setFont(font));
                doc.add(new Paragraph(" "));
            }
        }
        doc.add(new Paragraph(" "));
    }

    private void addScaleResults(Document doc, PdfFont font, PdfFont boldFont, ClinicalDay day) {
        doc.add(new Paragraph("CLINICAL SCALES")
                .setFont(boldFont).setFontSize(13).setUnderline());

        List<ScaleResult> scales = scaleResultRepository.findByClinicalDayId(day.getId());
        if (scales.isEmpty()) {
            doc.add(new Paragraph("  No scale results.").setFont(font));
        } else {
            Table table = new Table(UnitValue.createPercentArray(new float[]{30, 20, 25, 25}))
                    .useAllAvailableWidth();
            addTableHeader(table, font, "Scale", "Result", "Calculated At", "By");
            Map<UUID, String> userNames = getScaleUserNames(scales);
            for (ScaleResult sr : scales) {
                addTableRow(table, font,
                        sr.getScale().getName(),
                        sr.getResult(),
                        sr.getCalculatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        userNames.getOrDefault(sr.getCalculatedBy(),
                                sr.getCalculatedBy().toString().substring(0, 8)));
            }
            doc.add(table);
        }
        doc.add(new Paragraph(" "));
    }

    private void addSignatures(Document doc, PdfFont font, PdfFont boldFont, ClinicalDay day) {
        doc.add(new Paragraph("SIGNATURES")
                .setFont(boldFont).setFontSize(13).setUnderline());

        List<Signature> signatures = signatureRepository.findByClinicalDayId(day.getId());
        if (signatures.isEmpty()) {
            doc.add(new Paragraph("  No signatures.").setFont(font));
        } else {
            Table table = new Table(UnitValue.createPercentArray(new float[]{16, 20, 20, 22, 22}))
                    .useAllAvailableWidth();
            addTableHeader(table, font, "Role", "User", "Signed At", "Status", "Hash");
            Map<UUID, String> userNames = getSignatureUserNames(signatures);
            for (Signature sig : signatures) {
                addTableRow(table, font,
                        sig.getRole(),
                        userNames.getOrDefault(sig.getUserId(),
                                sig.getUserId().toString().substring(0, 8)),
                        sig.getSignedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        sig.getStatus(),
                        sig.getHash() != null ? sig.getHash().substring(0, Math.min(16, sig.getHash().length())) : "");
            }
            doc.add(table);
        }
        doc.add(new Paragraph(" "));
    }

    private void addFooter(Document doc, PdfFont font, int version, UUID userId) {
        doc.add(new Paragraph("— End of Document —")
                .setFont(font).setFontSize(10).setHorizontalAlignment(HorizontalAlignment.CENTER));
        doc.add(new Paragraph("Document v" + version + " | Generated by: " + userId
                + " | " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .setFont(font).setFontSize(8).setHorizontalAlignment(HorizontalAlignment.CENTER));
    }

    private Paragraph keyValue(String key, String value, PdfFont font) {
        return new Paragraph("  " + key + ": " + value).setFont(font).setFontSize(10);
    }

    private void addTableHeader(Table table, PdfFont font, String... headers) {
        for (String header : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(header).setFont(font).setFontSize(8).setBold()));
        }
    }

    private void addTableRow(Table table, PdfFont font, String... values) {
        for (String value : values) {
            table.addCell(new Cell().add(new Paragraph(value).setFont(font).setFontSize(7)));
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

    private Map<UUID, String> getUserNames(List<OrderExecution> executions) {
        Map<UUID, String> names = new HashMap<>();
        for (OrderExecution exec : executions) {
            if (!names.containsKey(exec.getExecutedBy())) {
                names.put(exec.getExecutedBy(), lookupUserName(exec.getExecutedBy()));
            }
        }
        return names;
    }

    private Map<UUID, String> getAuthorNames(List<MedicalNote> notes) {
        Map<UUID, String> names = new HashMap<>();
        for (MedicalNote note : notes) {
            if (!names.containsKey(note.getAuthorId())) {
                names.put(note.getAuthorId(), lookupUserName(note.getAuthorId()));
            }
        }
        return names;
    }

    private Map<UUID, String> getScaleUserNames(List<ScaleResult> scales) {
        Map<UUID, String> names = new HashMap<>();
        for (ScaleResult sr : scales) {
            if (!names.containsKey(sr.getCalculatedBy())) {
                names.put(sr.getCalculatedBy(), lookupUserName(sr.getCalculatedBy()));
            }
        }
        return names;
    }

    private Map<UUID, String> getSignatureUserNames(List<Signature> signatures) {
        Map<UUID, String> names = new HashMap<>();
        for (Signature sig : signatures) {
            if (!names.containsKey(sig.getUserId())) {
                names.put(sig.getUserId(), lookupUserName(sig.getUserId()));
            }
        }
        return names;
    }

    private String lookupUserName(UUID userId) {
        try {
            return userRepository.findById(userId)
                    .map(User::getFullName)
                    .orElse(userId.toString().substring(0, 8));
        } catch (Exception e) {
            return userId.toString().substring(0, 8);
        }
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
                .build();
    }
}
