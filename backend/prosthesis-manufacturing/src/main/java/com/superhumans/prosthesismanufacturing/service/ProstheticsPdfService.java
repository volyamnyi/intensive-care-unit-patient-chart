package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.superhumans.mis.dto.BookingMisDTO;
import com.superhumans.mis.dto.DepartmentDTO;
import com.superhumans.mis.dto.PatientInfoMisDTO;
import com.superhumans.mis.dto.ServiceMisDTO;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.GateDecision;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private static final List<String> UPPER_LIMB_FITTINGS = List.of(
            "B-TR  зняття мірок",
            "B-TR  примірка тестової гільзи",
            "B-TR  примірка тестової гільзи",
            "B-TR  примірка постійної внутрішньої гільзи",
            "B-TR  примірка постійної внутрішньої гільзи",
            "B-TR  примірка постійного протезу",
            "B-TR  налаштування кріплення протезу",
            "B-TR  ввідне навчання керуванню протезом",
            "a/B-TR  видача протезу");

    private static final String INSTITUTION_NAME =
            "МЕДИЧНИЙ ЦЕНТР БЛАГОДІЙНОЇ ОРГАНІЗАЦІЇ \"БЛАГОДІЙНИЙ ФОНД \"СУПЕРЛЮДИ\"";
    private static final String INSTITUTION_LICENSE = "Ліцензія: МОЗ № 926 від 18.05.2023 р.";
    private static final String INSTITUTION_NAME_2 = "Медичний центр БО «БФ «СУПЕРЛЮДИ»";
    private static final String INSTITUTION_ADDRESS = "79495, Львівська область, Львівський р-н, м. Винники, вул. Івасюка, буд. 31";
    private static final String INSTITUTION_EDRPOU = "Код за ЄДРПОУ: 44803597";
    private static final String APPROVE_ORG = "БО «БФ «СУПЕРЛЮДИ»";
    private static final String FOOTER_1 = "Superhumans.Center";
    private static final String FOOTER_2 = "superhumans.center";
    private static final String FOOTER_3 = "www.superhumans.com";
    private static final String FOOTER_4 = "help@superhumans.com";

    private static final com.itextpdf.kernel.colors.Color BLUE =
            new com.itextpdf.kernel.colors.DeviceRgb(0x3D, 0x7A, 0xBE);

    public byte[] generateOrderRecipe(ProstheticsOrder order) {
        return generateOrderRecipe(order, MisOrderTemplateData.builder().build());
    }

    public byte[] generateOrderRecipe(ProstheticsOrder order, MisOrderTemplateData misData) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Document doc = newOrderDocument(out)) {
            PdfFont font = loadFont();
            PdfFont bold = loadBoldFont(font);
            writeOrderPageOne(doc, font, bold, order, misData);
            if (order.getProductType() == ProductType.UPPER_LIMB) {
                doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                writeFittingsPage(doc, font, bold, order, misData);
            }
        }
        return out.toByteArray();
    }

    private Document newOrderDocument(ByteArrayOutputStream out) {
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(2, 14, 0, 14);
        return doc;
    }

    // ==================== PAGE 1 — ЗАМОВЛЕННЯ ====================

    private void writeOrderPageOne(Document doc, PdfFont font, PdfFont bold,
                                   ProstheticsOrder order, MisOrderTemplateData misData) {
        ProstheticsPatient patient = order.getPatient();
        PatientInfoMisDTO misPatient = misData == null ? null : misData.getPatientInfo();
        DepartmentDTO misCompany = misData == null ? null : misData.getCompany();

        // Логотип зверху
        Paragraph logo = new Paragraph()
                .setFont(bold).setFontSize(11)
                .setTextAlignment(TextAlignment.RIGHT);
        logo.add(new Text("superhumans "));
        logo.add(new Text("Center").setFont(font).setFontSize(5));
        doc.add(logo);

        // Шапка: організація | логотип | затвердження
        Table header = new Table(UnitValue.createPercentArray(new float[]{44, 16, 40})).useAllAvailableWidth();
        header.addCell(institutionCell(font, bold, misCompany));
        header.addCell(logoCell(font, bold));
        header.addCell(approvalCell(font, bold, order));
        doc.add(header);
        doc.add(new Paragraph("").setFontSize(4));

        // Title
        doc.add(new Paragraph("ЗАМОВЛЕННЯ № " + value(order.getOrderNumber()))
                .setFont(bold).setFontSize(12).setTextAlignment(TextAlignment.CENTER));
        String limbLabel = order.getProductType() == ProductType.UPPER_LIMB
                ? "на протези верхніх кінцівок" : "на протез";
        doc.add(new Paragraph(limbLabel)
                .setFont(bold).setFontSize(11).setTextAlignment(TextAlignment.CENTER));

        // Дата та особова картка
        Table dateRow = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        dateRow.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("Дата " + fmtDate(order.getPrescriptionDate()))
                        .setFont(font).setFontSize(7.5f)));
        dateRow.addCell(new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("До особової картки особи № " + patientId(order))
                        .setFont(font).setFontSize(7.5f)));
        doc.add(dateRow);

        // 1. Загальні відомості про особу
        sectionLabel(doc, bold, "Загальні відомості про особу");
        doc.add(line(font, "Прізвище, ім'я, по батькові", misPatientName(patient, misPatient)));
        doc.add(line(font, "Дата народження", misPatientBirth(patient, misPatient)));
        doc.add(line(font, "Зареєстроване або задеклароване місце проживання (перебування)",
                misPatientAddress(patient, misPatient)));
        doc.add(line(font, "Контактні телефони", misPatientPhone(patient, misPatient)));
        doc.add(line(font, "Електронна пошта", misPatientEmail(patient, misPatient)));
        doc.add(new Paragraph("Стать: " + (patient == null ? "—" : genderLabel(patient.getGender()))
                + "   Зріст " + (patient == null || patient.getHeightCm() == null ? "—" : patient.getHeightCm() + " см")
                + "   Вага " + (patient == null || patient.getWeightKg() == null ? "—" : patient.getWeightKg() + " кг"))
                .setFont(font).setFontSize(6.5f).setMultipliedLeading(1.0f));
        doc.add(line(font, "Соціальний статус:", patient == null ? "—" : patient.getSocialStatus()));

        // 2. Причина та рівень порушень кінцівки, стан кукси
        doc.add(new Paragraph("Причина та рівень порушень кінцівки, стан кукси")
                .setFont(bold).setFontSize(9.5f).setTextAlignment(TextAlignment.CENTER).setMarginTop(4));

        doc.add(line(font, "Причина ураження:", patient == null ? "—" : patient.getCause()));
        doc.add(line(font, "Дата ампутації", fmtDate(patient == null ? null : patient.getAmputationDate())));
        doc.add(line(font, "Уражена кінцівка", patient == null ? "—" : limbLabel(patient.getAffectedLimb())));
        doc.add(line(font, "Рівень ушкодження:", value(order.getAmputationLevel())));
        doc.add(line(font, "Стан кукси: Форма:", clinicalValue(order, "stump_form")));
        doc.add(line(font, "Стан м'яких тканин та шкіри:", stumpText(order)));
        doc.add(line(font, "Післяопераційний рубець:", clinicalValue(order, "scar")));
        doc.add(line(font, "Чутливість:", clinicalValue(order, "sensitivity")));
        doc.add(line(font, "Наявність болю:", clinicalValue(order, "pain")));
        doc.add(line(font, "Об'єм рухів у суглобах ураженої кінцівки:", clinicalValue(order, "rom")));
        doc.add(new Paragraph("У разі наявності обмежень чи контрактури вказати суглоб і порушення рухомості "
                + "(згинання / розгинання, приведення / відведення та інші)")
                .setFont(font).setFontSize(6.5f));
        doc.add(line(font, "Сила м'язів ураженої верхньої кінцівки:", clinicalValue(order, "muscle_strength")));
        doc.add(new Paragraph("У разі наявності зниження сили м'язів вказати певні порушення рухливості")
                .setFont(font).setFontSize(6.5f));

        // 3. Загальний стан здоров'я (жирний підзаголовок)
        doc.add(new Paragraph("Загальний стан здоров'я:").setFont(bold).setFontSize(7f).setMarginTop(2));
        doc.add(line(font, "Стан опорно-рухової системи:", clinicalValue(order, "musculoskeletal")));
        doc.add(line(font, "Стан контрлатеральної верхньої кінцівки:", clinicalValue(order, "contralateral")));
        doc.add(line(font, "Стан тулуба:", clinicalValue(order, "torso")));
        doc.add(line(font, "Стан інших систем організму:", clinicalValue(order, "other_systems")));
        doc.add(line(font, "Порушення інших систем організму та/або органів, які можуть вплинути на протезування:",
                clinicalValue(order, "complications")));

        // 4. Активність і залучення до життєвих ситуацій особи
        doc.add(new Paragraph("Активність і залучення до життєвих ситуацій особи")
                .setFont(bold).setFontSize(7f).setMarginTop(2));
        doc.add(line(font, "Діяльність особи пов'язана з", clinicalValue(order, "activity_type")));
        doc.add(line(font, "Залучення до життєвих ситуацій, додаткова діяльність і хобі",
                clinicalValue(order, "hobbies")));

        // 5. Здатність до самообслуговування
        doc.add(new Paragraph("Здатність до самообслуговування").setFont(bold).setFontSize(7f).setMarginTop(2));
        doc.add(line(font, "спроможність самостійно митися, доглядати за частинами тіла",
                clinicalValue(order, "selfcare_wash")));
        doc.add(line(font, "спроможність самостійно одягатися", clinicalValue(order, "selfcare_dress")));
        doc.add(line(font, "спроможність самостійно вживати їжу, напої", clinicalValue(order, "selfcare_eat")));

        // 6. Діагноз та виріб
        doc.add(line(font, "Діагноз по типу конструкції протезу:",
                value(order.getAmputationLevel()) + " " + misProductCode(order, misData))
                .setMarginTop(4));
        doc.add(new Paragraph("").setFontSize(2));
        doc.add(line(font, "Найменування виробу (засобу реабілітації) та код з згідно ISO 9999:2016, IDT):",
                misProductCode(order, misData)));
        doc.add(new Paragraph(misProductDesc(order, misData) + ".B-TR.c — протези передпліччя з тяговим керуванням комбіновані")
                .setFont(font).setFontSize(6.5f).setMarginBottom(2));

        // 7. Підписи: Лікар / Технік
        Table signTable = new Table(UnitValue.createPercentArray(new float[]{20, 45, 35})).useAllAvailableWidth();
        signTable.addCell(signRoleCell(font, bold, "Лікар"));
        signTable.addCell(signNameCell(font, bold, misDoctorName(order, misData)));
        signTable.addCell(signLineCell(font, "(підпис)_____________"));
        signTable.addCell(signRoleCell(font, bold, "Технік"));
        signTable.addCell(signNameCell(font, bold, misTechnicianName(misData)));
        signTable.addCell(signLineCell(font, "(підпис)_____________"));
        signTable.setMarginTop(2);
        doc.add(signTable);

        // 8. Ознайомлення замовника + дата передання
        doc.add(new Paragraph("Із призначенням ознайомлений(на) "
                + (patient == null ? "—" : patient.getPib())
                + "  (Власне ім'я ПРІЗВИЩЕ замовника)  (підпис)____________")
                .setFont(font).setFontSize(6.5f).setMarginTop(2));
        doc.add(new Paragraph("Дата передання виробу у виробництво " + fmtDate(order.getPrescriptionDate()))
                .setFont(font).setFontSize(6.5f).setMarginTop(2));

        footer(doc, font, bold);
    }

    // ==================== PAGE 2 — КОМПЛЕКТУВАЛЬНІ ВИРОБИ ТА ПРИМІРКИ ====================

    private void writeFittingsPage(Document doc, PdfFont font, PdfFont bold,
                                   ProstheticsOrder order, MisOrderTemplateData misData) {
        // Заголовок
        doc.add(new Paragraph("До замовлення № " + value(order.getOrderNumber()))
                .setFont(bold).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("").setFontSize(4));

        // Дата прийняття виробу у роботу (синій)
        doc.add(new Paragraph("Дата прийняття виробу у роботу :")
                .setFont(font).setFontSize(8).setFontColor(BLUE));
        doc.add(new Paragraph("").setFontSize(4));

        // Комплектувальні вироби та матеріали
        doc.add(new Paragraph("Комплектувальні вироби та матеріали")
                .setFont(bold).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("").setFontSize(2));
        materialsTable(doc, font, bold, order, misData);
        doc.add(new Paragraph("").setFontSize(8));

        // Технік
        Paragraph tech = new Paragraph().setFont(font).setFontSize(8);
        tech.add(new Text("Технік ").setFontColor(BLUE));
        tech.add(new Text("________________________________  "));
        tech.add(new Text("(Власне ім'я ПРІЗВИЩЕ)  "));
        tech.add(new Text("(підпис)"));
        doc.add(tech);
        doc.add(new Paragraph("").setFontSize(6));

        // Примірки
        doc.add(new Paragraph("Примірки")
                .setFont(bold).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("").setFontSize(2));
        fittingsTable(doc, font, bold, order);
        doc.add(new Paragraph("").setFontSize(4));

        // Дата видачі протезу
        doc.add(new Paragraph("Дата видачі протезу")
                .setFont(font).setFontSize(8));
        doc.add(new Paragraph("").setFontSize(2));

        // Виріб — отримав (рамка)
        Table receiptBox = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell receiptCell = new Cell().setBorder(new SolidBorder(BLUE, 0.6f)).setPadding(3);
        receiptCell.add(new Paragraph("Виріб " + receivedVerb(order) + ",  "
                + misPatientName(order.getPatient(),
                        misData == null ? null : misData.getPatientInfo()))
                .setFont(font).setFontSize(8));
        receiptCell.add(new Paragraph("(Власне ім'я ПРІЗВИЩЕ замовника)                (підпис)")
                .setFont(font).setFontSize(7).setMarginLeft(35));
        receiptBox.addCell(receiptCell);
        doc.add(receiptBox);
        doc.add(new Paragraph("").setFontSize(8));

        // Начальник відділу протезування верхніх кінцівок
        Paragraph chief = new Paragraph().setFont(font).setFontSize(8);
        chief.add(new Text("Начальник відділу протезування верхніх кінцівок :  "));
        chief.add(new Text("Нагорний Денис Віталійович").setFontColor(BLUE).setUnderline());
        doc.add(chief);
        doc.add(new Paragraph("").setFontSize(8));

        // (Дата) : (Підпис)
        Paragraph finalRow = new Paragraph().setFont(font).setFontSize(8).setMarginLeft(50);
        finalRow.add(new Text("(Дата) : ").setFontColor(BLUE));
        finalRow.add(new Text("     "));
        finalRow.add(new Text("(Підпис) ").setFontColor(BLUE));
        finalRow.add(new Text("__________________"));
        doc.add(finalRow);

        // Нумерація сторінок
        Paragraph pager = new Paragraph()
                .setFont(font).setFontSize(7)
                .setTextAlignment(TextAlignment.RIGHT);
        pager.add(new Text("Сторінка "));
        pager.add(new Text("1").setFontColor(BLUE));
        pager.add(new Text(" з "));
        pager.add(new Text("1").setFontColor(BLUE));
        doc.add(pager);
    }

    // ==================== NEW HELPER METHODS ====================

    private String patientId(ProstheticsOrder order) {
        if (order.getPatient() == null || order.getPatient().getId() == null) {
            return "—";
        }
        return String.valueOf(order.getPatient().getId());
    }

    private String receivedVerb(ProstheticsOrder order) {
        if (order.getPatient() == null || order.getPatient().getGender() == null) {
            return "отримав";
        }
        return switch (order.getPatient().getGender().toUpperCase(Locale.ROOT)) {
            case "FEMALE" -> "отримала";
            default -> "отримав";
        };
    }

    private void materialsTable(Document doc, PdfFont font, PdfFont bold,
                                ProstheticsOrder order, MisOrderTemplateData misData) {
        Table materials = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20}))
                .useAllAvailableWidth();
        addHeaderRow(materials, font, bold, "Найменування", "Артикул", "Одиниця виміру", "Кількість");
        List<MaterialItem> items = materialItems(order.getMaterials());
        if (items.isEmpty() && !misBookings(misData).isEmpty()) {
            for (BookingMisDTO b : misBookings(misData)) {
                items.add(new MaterialItem(
                        value(b.getBookingName()),
                        value(b.getServiceCode()),
                        "шт",
                        b.getBookingQuantity() == null ? "1" : String.valueOf(b.getBookingQuantity())));
            }
        }
        if (items.isEmpty()) {
            items = List.of(new MaterialItem("—", "", "", ""));
        }
        for (MaterialItem item : items) {
            materials.addCell(dataCell(font, item.name()));
            materials.addCell(dataCell(font, item.articul()));
            materials.addCell(dataCell(font, item.unit()));
            materials.addCell(dataCell(font, item.qty()));
        }
        doc.add(materials);
    }

    private void fittingsTable(Document doc, PdfFont font, PdfFont bold, ProstheticsOrder order) {
        Table fittings = new Table(UnitValue.createPercentArray(new float[]{8, 22, 70}))
                .useAllAvailableWidth();
        addHeaderRow(fittings, font, bold, "№", "Дата примірки", "Послуга");
        int index = 1;
        for (String service : UPPER_LIMB_FITTINGS) {
            fittings.addCell(dataCell(font, String.valueOf(index++)));
            fittings.addCell(dataCell(font, ""));
            fittings.addCell(dataCell(font, service));
        }
        doc.add(fittings);
    }

    // ==================== PAGE 1 helpers ====================

    private Cell institutionCell(PdfFont font, PdfFont bold, DepartmentDTO misCompany) {
        Cell cell = new Cell().setBorder(new SolidBorder(0.6f)).setPadding(1)
                .setTextAlignment(TextAlignment.CENTER);
        if (misCompany != null && misCompany.getName() != null && !misCompany.getName().isBlank()) {
            cell.add(new Paragraph(misCompany.getName()).setFont(bold).setFontSize(6.5f));
        } else {
            cell.add(new Paragraph(INSTITUTION_NAME).setFont(bold).setFontSize(6.5f));
        }
        cell.add(new Paragraph(INSTITUTION_LICENSE).setFont(bold).setFontSize(6.5f));
        cell.add(new Paragraph(INSTITUTION_NAME_2).setFont(bold).setFontSize(6.5f));
        if (misCompany != null && misCompany.getAddress() != null && !misCompany.getAddress().isBlank()) {
            cell.add(new Paragraph(misCompany.getAddress()).setFont(bold).setFontSize(6.5f));
        } else {
            cell.add(new Paragraph(INSTITUTION_ADDRESS).setFont(bold).setFontSize(6.5f));
        }
        cell.add(new Paragraph(INSTITUTION_EDRPOU).setFont(bold).setFontSize(6.5f));
        return cell;
    }

    private Cell logoCell(PdfFont font, PdfFont bold) {
        Cell cell = new Cell().setBorder(new SolidBorder(0.6f)).setPadding(2)
                .setTextAlignment(TextAlignment.CENTER);
        Paragraph logo = new Paragraph().setFont(bold).setFontSize(10);
        logo.add(new Text("superhumans "));
        logo.add(new Text("Center").setFont(font).setFontSize(4.5f));
        cell.add(logo);
        return cell;
    }

    private Cell approvalCell(PdfFont font, PdfFont bold, ProstheticsOrder order) {
        Cell cell = new Cell().setBorder(new SolidBorder(0.6f)).setPadding(1)
                .setTextAlignment(TextAlignment.CENTER);
        cell.add(new Paragraph("Замовлення на протези верхніх кінцівок")
                .setFont(font).setFontSize(6f));
        cell.add(new Paragraph("ЗАТВЕРДЖЕНО").setFont(bold).setFontSize(6.5f));
        cell.add(new Paragraph(APPROVE_ORG).setFont(bold).setFontSize(6.5f));
        // Код-таблиця: 0 2 . 1 2 . 2 0 2 5 р. № 42
        Table code = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1.4f, 1, 1}))
                .setMarginTop(2).setMarginLeft(18).setWidth(UnitValue.createPercentValue(80));
        String[] digits = {"0", "2", ".", "1", "2", ".", "2", "0", "2", "5", "р.", "№", "4", "2"};
        for (int i = 0; i < digits.length; i++) {
            Cell d = new Cell().setBorder(new SolidBorder(0.6f)).setPadding(0)
                    .setTextAlignment(TextAlignment.CENTER);
            d.add(new Paragraph(digits[i]).setFont(bold).setFontSize(6f));
            if (i == 11) {
                d.setBorder(Border.NO_BORDER);
            }
            code.addCell(d);
        }
        cell.add(code);
        return cell;
    }

    // ==================== PAGE 1 helpers ====================

    private void footer(Document doc, PdfFont font, PdfFont bold) {
        Paragraph foot = new Paragraph()
                .setFont(font).setFontSize(6f);
        foot.add(new Text("Superhumans.Center     "));
        foot.add(new Text("superhumans.center     "));
        foot.add(new Text("www.superhumans.com     "));
        foot.add(new Text("help@superhumans.com"));
        doc.add(foot);
    }

    private void sectionLabel(Document doc, PdfFont bold, String text) {
        doc.add(new Paragraph(text).setFont(bold).setFontSize(8.5f).setMarginLeft(26));
    }

    private Paragraph line(PdfFont font, String key, String value) {
        return new Paragraph(key + " " + value).setFont(font).setFontSize(6.5f)
                .setMultipliedLeading(1.0f);
    }

    private Cell signRoleCell(PdfFont font, PdfFont bold, String text) {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(0)
                .add(new Paragraph(text).setFont(font).setFontSize(6.5f));
    }

    private Cell signNameCell(PdfFont font, PdfFont bold, String text) {
        Cell cell = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
        cell.add(new Paragraph(text + "  (Власне ім'я ПРІЗВИЩЕ)")
                .setFont(font).setFontSize(6.5f));
        return cell;
    }

    private Cell signLineCell(PdfFont font, String text) {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(0)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(text).setFont(font).setFontSize(6.5f));
    }

    private void addHeaderRow(Table table, PdfFont font, PdfFont bold, String... headers) {
        for (String header : headers) {
            table.addCell(new Cell().setBorder(new SolidBorder(BLUE, 0.6f)).setPadding(2)
                    .add(new Paragraph(header).setFont(bold).setFontSize(7.5f)
                            .setFontColor(BLUE)
                            .setTextAlignment(TextAlignment.CENTER)));
        }
    }

    private Cell dataCell(PdfFont font, String text) {
        return new Cell().setBorder(new SolidBorder(BLUE, 0.6f)).setPadding(2)
                .add(new Paragraph(text == null ? "" : text).setFont(font).setFontSize(7.5f));
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String fmtDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FORMAT);
    }

    private String genderLabel(String gender) {
        if (gender == null || gender.isBlank()) {
            return "—";
        }
        return switch (gender.toUpperCase(Locale.ROOT)) {
            case "MALE" -> "Чоловіча";
            case "FEMALE" -> "Жіноча";
            default -> gender;
        };
    }

    private String limbLabel(String limb) {
        if (limb == null || limb.isBlank()) {
            return "—";
        }
        try {
            return switch (LimbSide.valueOf(limb.toUpperCase(Locale.ROOT))) {
                case LEFT -> "Ліва";
                case RIGHT -> "Права";
            };
        } catch (IllegalArgumentException e) {
            return limb;
        }
    }

    // ==================== MIS fallback helpers ====================
    // MIS data takes precedence; falls back to locally stored patient/order data.

    private String misPatientName(ProstheticsPatient patient, PatientInfoMisDTO mis) {
        if (mis != null && mis.getPatientName() != null && !mis.getPatientName().isBlank()) {
            return mis.getPatientName();
        }
        return patient == null ? "—" : value(patient.getPib());
    }

    private String misPatientBirth(ProstheticsPatient patient, PatientInfoMisDTO mis) {
        if (mis != null && mis.getPatientBirthDate() != null) {
            return fmtDate(mis.getPatientBirthDate());
        }
        return fmtDate(patient == null ? null : patient.getBirthDate());
    }

    private String misPatientAddress(ProstheticsPatient patient, PatientInfoMisDTO mis) {
        if (mis != null && mis.getPatientAddress() != null && !mis.getPatientAddress().isBlank()) {
            return mis.getPatientAddress();
        }
        return patient == null ? "—" : value(patient.getResidence());
    }

    private String misPatientPhone(ProstheticsPatient patient, PatientInfoMisDTO mis) {
        if (mis != null && mis.getPatientPhone() != null && !mis.getPatientPhone().isBlank()) {
            return mis.getPatientPhone();
        }
        return patient == null ? "—" : value(patient.getPhone());
    }

    private String misPatientEmail(ProstheticsPatient patient, PatientInfoMisDTO mis) {
        if (mis != null && mis.getPatientEmail() != null && !mis.getPatientEmail().isBlank()) {
            return mis.getPatientEmail();
        }
        return patient == null ? "—" : value(patient.getEmail());
    }

    private String misProductCode(ProstheticsOrder order, MisOrderTemplateData misData) {
        if (misData != null && misData.getServices() != null && !misData.getServices().isEmpty()) {
            ServiceMisDTO first = misData.getServices().get(0);
            if (first.getServiceCode() != null && !first.getServiceCode().isBlank()) {
                return first.getServiceCode();
            }
        }
        return value(order.getProductCode());
    }

    private String misProductDesc(ProstheticsOrder order, MisOrderTemplateData misData) {
        if (misData != null && misData.getServices() != null && !misData.getServices().isEmpty()) {
            ServiceMisDTO first = misData.getServices().get(0);
            if (first.getServiceName() != null && !first.getServiceName().isBlank()) {
                return first.getServiceName();
            }
        }
        return value(order.getProductCode());
    }

    private String misDoctorName(ProstheticsOrder order, MisOrderTemplateData misData) {
        if (misData != null && misData.getDoctorName() != null && !misData.getDoctorName().isBlank()) {
            return misData.getDoctorName();
        }
        return value(order.getDoctorName());
    }

    private String misTechnicianName(MisOrderTemplateData misData) {
        if (misData != null && misData.getTechnicianName() != null && !misData.getTechnicianName().isBlank()) {
            return misData.getTechnicianName();
        }
        return "Нагорний Д.В";
    }

    private List<BookingMisDTO> misBookings(MisOrderTemplateData misData) {
        if (misData == null || misData.getBookings() == null) {
            return List.of();
        }
        return misData.getBookings();
    }

    private String clinicalValue(ProstheticsOrder order, String key) {
        ProstheticsPatient patient = order.getPatient();
        if (patient == null || patient.getClinicalState() == null || patient.getClinicalState().isBlank()) {
            return "—";
        }
        try {
            JsonNode root = objectMapper.readTree(patient.getClinicalState());
            JsonNode node = root.get(key);
            return node == null || node.isNull() || node.asText().isBlank() ? "—" : node.asText();
        } catch (Exception e) {
            return "—";
        }
    }

    private String stumpText(ProstheticsOrder order) {
        ProstheticsPatient patient = order.getPatient();
        if (patient == null || patient.getStump() == null || patient.getStump().isBlank()) {
            return "—";
        }
        try {
            JsonNode root = objectMapper.readTree(patient.getStump());
            if (root.isArray()) {
                List<String> parts = new ArrayList<>();
                for (JsonNode node : root) {
                    if (node.has("label")) {
                        parts.add(node.get("label").asText() + ": " + node.path("value").asText());
                    }
                }
                return parts.isEmpty() ? "—" : String.join("; ", parts);
            }
            if (root.isObject()) {
                Map<String, String> parts = new LinkedHashMap<>();
                putStumpPart(parts, root, "form", "Форма кукси", Map.of(
                        "cylindrical", "циліндрична", "conical", "конічна"));
                putStumpPart(parts, root, "soft_tissue", "М'які тканини", Map.of(
                        "healthy", "здорові", "scarred", "рубцеві зміни"));
                putStumpPart(parts, root, "skin", "Шкіра", Map.of(
                        "intact", "неушкоджена", "scarred", "рубцева"));
                putStumpPart(parts, root, "sensitivity", "Чутливість", Map.of(
                        "normal", "нормальна", "reduced", "знижена"));
                if (root.has("pain")) {
                    parts.put("Біль", root.get("pain").asBoolean(false) ? "присутній" : "відсутній");
                }
                return parts.isEmpty() ? "—" : String.join("; ", parts.values());
            }
            return "—";
        } catch (Exception e) {
            return "—";
        }
    }

    private void putStumpPart(Map<String, String> parts, JsonNode root, String key, String label,
                              Map<String, String> translation) {
        JsonNode node = root.get(key);
        if (node == null || node.isNull()) {
            return;
        }
        String raw = node.asText();
        parts.put(label, translation.getOrDefault(raw.toLowerCase(Locale.ROOT), raw));
    }

    private record MaterialItem(String name, String articul, String unit, String qty) {
    }

    private List<MaterialItem> materialItems(String materialsJson) {
        if (materialsJson == null || materialsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(materialsJson);
            JsonNode items = root.isArray() ? root : root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                return List.of(new MaterialItem(materialsJson, "", "", ""));
            }
            List<MaterialItem> result = new ArrayList<>();
            for (JsonNode node : items) {
                if (node.isTextual()) {
                    result.add(new MaterialItem(node.asText(), "", "", ""));
                } else if (node.isObject()) {
                    JsonNode articul = node.has("articul") ? node.get("articul") : node.get("article");
                    result.add(new MaterialItem(
                            node.has("name") ? node.get("name").asText() : "",
                            articul == null || articul.isNull() ? "" : articul.asText(),
                            node.has("unit") && !node.get("unit").isNull() ? node.get("unit").asText() : "",
                            node.has("qty") && !node.get("qty").isNull() ? node.get("qty").asText() : ""));
                }
            }
            return result;
        } catch (Exception e) {
            return List.of(new MaterialItem(materialsJson, "", "", ""));
        }
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
