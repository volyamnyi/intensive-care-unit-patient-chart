package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.GateDecision;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ResourceUsage;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotElement;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProstheticsPdfService {

    ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public byte[] generateOrderRecipe(ProstheticsOrder order) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = newDocument(out)) {
            PdfFont font = loadFont();
            PdfFont bold = loadBoldFont(font);
            title(doc, bold, "РЕЦЕПТ НА ВИГОТОВЛЕННЯ ПРОТЕЗА");
            infoLine(doc, font, "Номер замовлення", order.getOrderNumber());
            infoLine(doc, font, "Дата призначення",
                    order.getPrescriptionDate() == null ? "—"
                            : order.getPrescriptionDate().format(DATE_FORMAT));
            infoLine(doc, font, "Лікар", order.getDoctorName() == null ? "—" : order.getDoctorName());
            infoLine(doc, font, "Пацієнт", patientName(order));
            infoLine(doc, font, "Тип протеза", order.getProsthesisType() == null ? "—"
                    : order.getProsthesisType());
            infoLine(doc, font, "Рівень ампутації", order.getAmputationLevel() == null ? "—"
                    : order.getAmputationLevel());
            infoLine(doc, font, "Сторона", order.getLimbSide() == null ? "—"
                    : order.getLimbSide().name().toLowerCase(Locale.ROOT));
            infoLine(doc, font, "Категорія виробу", order.getProductType() == null ? "—"
                    : order.getProductType().name());
            doc.add(new Paragraph("Матеріали:"));
            for (String material : parseMaterials(order.getMaterials())) {
                doc.add(new Paragraph("• " + material).setFont(font).setFontSize(10));
            }
            doc.add(new Paragraph("").setFont(font).setFontSize(8));
            doc.add(new Paragraph("Документ сформовано автоматично системою управління "
                    + "технологічним процесом виготовлення протезів")
                    .setFont(font).setFontSize(8));
        }
        return out.toByteArray();
    }

    public byte[] generateFinalReport(FlowInstance instance, ProstheticsOrder order,
                                      SnapshotTemplate snapshot, List<StepExecution> executions,
                                      List<GateDecision> decisions, List<ResourceUsage> resources) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = newDocument(out)) {
            PdfFont font = loadFont();
            PdfFont bold = loadBoldFont(font);
            title(doc, bold, "ЗВІТ ПРО ВИКОНАННЯ ТЕХНОЛОГІЧНОГО ПРОЦЕСУ");
            infoLine(doc, font, "Екземпляр процесу", instance.getId().toString());
            infoLine(doc, font, "Замовлення", order.getOrderNumber());
            infoLine(doc, font, "Пацієнт", patientName(order));
            infoLine(doc, font, "Шаблон", snapshot.getName() == null ? "—"
                    : snapshot.getName() + " v" + snapshot.getVersion());
            infoLine(doc, font, "Статус", instance.getStatus() == null ? "—" : instance.getStatus().name());
            infoLine(doc, font, "Початок", fmtDateTime(instance.getStartTime()));
            infoLine(doc, font, "Завершення", fmtDateTime(instance.getEndTime()));
            infoLine(doc, font, "Активний час",
                    instance.getTotalActiveSeconds() == null ? "0" : formatDuration(instance.getTotalActiveSeconds()));
            infoLine(doc, font, "Простої",
                    instance.getTotalIdleSeconds() == null ? "0" : formatDuration(instance.getTotalIdleSeconds()));
            infoLine(doc, font, "Кількість доопрацювань",
                    String.valueOf(instance.getReworkCount() == null ? 0 : instance.getReworkCount()));

            Map<UUID, SnapshotStep> stepsById = indexSteps(snapshot);
            for (StepExecution execution : executions) {
                doc.add(new Paragraph("Крок «" + stepName(stepsById, execution.getStepId()) + "»"
                        + " (спроба " + execution.getAttemptNumber() + ", "
                        + execution.getStatus().name() + ")")
                        .setFont(bold).setFontSize(10));
                for (Map.Entry<String, String> entry : valueSummary(stepsById, execution).entrySet()) {
                    doc.add(new Paragraph(entry.getKey() + ": " + entry.getValue())
                            .setFont(font).setFontSize(10).setMarginLeft(12));
                }
            }
            if (decisions != null && !decisions.isEmpty()) {
                doc.add(new Paragraph("Рішення контролю якості").setFont(bold).setFontSize(11));
                for (GateDecision decision : decisions) {
                    doc.add(new Paragraph(decision.getDecision().name()
                            + (decision.getComment() == null || decision.getComment().isBlank() ? ""
                            : " — " + decision.getComment()))
                            .setFont(font).setFontSize(10).setMarginLeft(12));
                }
            }
            if (resources != null && !resources.isEmpty()) {
                doc.add(new Paragraph("Витрати матеріалів").setFont(bold).setFontSize(11));
                for (ResourceUsage usage : resources) {
                    doc.add(new Paragraph(usage.getMaterial() + ": "
                            + (usage.getQty() == null ? BigDecimal.ZERO : usage.getQty()) + " "
                            + (usage.getUnit() == null ? "" : usage.getUnit()))
                            .setFont(font).setFontSize(10).setMarginLeft(12));
                }
            }
            signatureLine(doc, font, "Виконавець (протезист)");
            signatureLine(doc, font, "Адміністратор виробництва");
        }
        return out.toByteArray();
    }

    public byte[] generateFailureReport(FlowInstance instance, ProstheticsOrder order,
                                        SnapshotTemplate snapshot, String category, String description) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = newDocument(out)) {
            PdfFont font = loadFont();
            PdfFont bold = loadBoldFont(font);
            title(doc, bold, "ЗВІТ ПРО НЕВИКОНАННЯ");
            infoLine(doc, font, "Екземпляр процесу", instance.getId().toString());
            infoLine(doc, font, "Замовлення", order.getOrderNumber());
            infoLine(doc, font, "Пацієнт", patientName(order));
            infoLine(doc, font, "Шаблон", snapshot.getName() == null ? "—"
                    : snapshot.getName() + " v" + snapshot.getVersion());
            infoLine(doc, font, "Категорія невідповідності", category == null ? "—" : category);
            infoLine(doc, font, "Опис", description == null ? "—" : description);
            infoLine(doc, font, "Статус", instance.getStatus() == null ? "—" : instance.getStatus().name());
            infoLine(doc, font, "Початок", fmtDateTime(instance.getStartTime()));
            infoLine(doc, font, "Завершення", fmtDateTime(instance.getEndTime()));
            signatureLine(doc, font, "Виконавець (протезист)");
        }
        return out.toByteArray();
    }

    private Document newDocument(ByteArrayOutputStream out) {
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 40, 40, 40);
        return doc;
    }

    private void title(Document doc, PdfFont bold, String text) {
        doc.add(new Paragraph(text).setFont(bold).setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("").setFontSize(6));
    }

    private void infoLine(Document doc, PdfFont font, String key, String value) {
        doc.add(new Paragraph(key + ": " + value).setFont(font).setFontSize(10));
    }

    private void signatureLine(Document doc, PdfFont font, String label) {
        doc.add(new Paragraph("").setFont(font).setFontSize(16));
        doc.add(new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth()
                .addCell(new com.itextpdf.layout.element.Cell()
                        .add(new Paragraph(label).setFont(font).setFontSize(10))));
        doc.add(new Paragraph("__________________________").setFont(font).setFontSize(10));
    }

    private String patientName(ProstheticsOrder order) {
        return order.getPatient() == null ? "—"
                : order.getPatient().getPib() + (order.getPatient().getBirthDate() == null ? ""
                : " (" + order.getPatient().getBirthDate().format(DATE_FORMAT) + ")");
    }

    private List<String> parseMaterials(String materialsJson) {
        if (materialsJson == null || materialsJson.isBlank()) {
            return List.of("—");
        }
        try {
            return objectMapper.readValue(materialsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of(materialsJson);
        }
    }

    private Map<UUID, SnapshotStep> indexSteps(SnapshotTemplate snapshot) {
        Map<UUID, SnapshotStep> map = new LinkedHashMap<>();
        if (snapshot.getStages() == null) {
            return map;
        }
        snapshot.getStages().forEach(stage -> {
            if (stage.getSteps() != null) {
                stage.getSteps().forEach(step -> map.put(step.getId(), step));
            }
        });
        return map;
    }

    private String stepName(Map<UUID, SnapshotStep> stepsById, UUID stepId) {
        SnapshotStep step = stepsById.get(stepId);
        return step == null || step.getName() == null ? stepId.toString() : step.getName();
    }

    private Map<String, String> valueSummary(Map<UUID, SnapshotStep> stepsById, StepExecution execution) {
        Map<String, String> result = new LinkedHashMap<>();
        if (execution.getValues() == null || execution.getValues().isBlank()) {
            return result;
        }
        Map<String, Object> values;
        try {
            values = objectMapper.readValue(execution.getValues(),
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    });
        } catch (Exception e) {
            return Map.of("Значення", execution.getValues());
        }
        SnapshotStep step = stepsById.get(execution.getStepId());
        Map<String, String> labels = new LinkedHashMap<>();
        if (step != null && step.getElements() != null) {
            for (SnapshotElement element : step.getElements()) {
                labels.put(element.getId().toString(), element.getLabel());
            }
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            result.put(labels.getOrDefault(entry.getKey(), entry.getKey()),
                    String.valueOf(entry.getValue()));
        }
        return result;
    }

    private String fmtDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "—" : dateTime.format(DATE_TIME_FORMAT);
    }

    private String formatDuration(Long seconds) {
        long total = seconds == null ? 0L : seconds;
        long hours = total / 3600;
        long minutes = (total % 3600) / 60;
        long secs = total % 60;
        return hours + " год " + minutes + " хв " + secs + " с";
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
            } catch (Exception ignored) {
            }
        }
        try {
            return PdfFontFactory.createFont("Helvetica");
        } catch (Exception e) {
            return null;
        }
    }

    private PdfFont loadBoldFont(PdfFont regular) {
        if (regular == null) {
            return null;
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
            } catch (Exception ignored) {
            }
        }
        return regular;
    }
}
