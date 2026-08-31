package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import com.superhumans.prosthesismanufacturing.entity.StepExecutionStatus;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotElement;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProstheticsPdfServiceTest {

    private final ProstheticsPdfService service = new ProstheticsPdfService(new ObjectMapper());

    private ProstheticsPatient upperLimbPatient() {
        return ProstheticsPatient.builder()
                .pib("Сніжко Іван Петрович")
                .birthDate(LocalDate.of(1991, 3, 14))
                .gender("Чоловіча")
                .heightCm(182)
                .weightKg(84)
                .socialStatus("Військовослужбовець")
                .residence("м. Миколаїв, вул. Чапаєва, буд. 54-А")
                .phone("380933329111")
                .email("snizhko.ivan@example.com")
                .healthStatus("задовільний")
                .cause("Мінно-вибухова травма")
                .amputationDate(LocalDate.of(2024, 11, 8))
                .affectedLimb("RIGHT")
                .amputationLevel("в/3 передпліччя")
                .amputationSite("вище кисті (верхня третина передпліччя)")
                .clinicalState("{\"rom\":\"Обсяг рухів у повному обсязі\",\"contractures\":\"відсутні\","
                        + "\"nerves\":\"задовільний\",\"vessels\":\"задовільний\","
                        + "\"motor_functions\":\"рухи в кисті відсутні\",\"healthy_limb\":\"без патологій\","
                        + "\"cardiovascular\":\"компенсований\",\"respiratory\":\"без патологій\","
                        + "\"digestive\":\"без патологій\",\"diseases\":\"відсутні\",\"activity\":\"в нормі\"}")
                .stump("[{\"label\":\"Форма кукси\",\"value\":\"Циліндрична\"},"
                        + "{\"label\":\"Довжина кукси, см\",\"value\":\"18\"}]")
                .build();
    }

    private ProstheticsOrder upperLimbOrder() {
        return ProstheticsOrder.builder()
                .orderNumber("PR-2026-0001")
                .patient(upperLimbPatient())
                .prosthesisType("Протез передпліччя")
                .productType(ProductType.UPPER_LIMB)
                .amputationLevel("в/3 передпліччя")
                .doctorName("Олександр Мельник")
                .prescriptionDate(LocalDate.of(2026, 7, 20))
                .materials("[{\"name\":\"Силіконовий чохол\",\"unit\":\"шт\",\"qty\":\"1\",\"articul\":\"SC-211\"},"
                        + "{\"name\":\"Поліпропілен\",\"unit\":\"кг\",\"qty\":\"0,8\",\"articul\":\"PP-04\"}]")
                .productCode("06 18 09.В-ТР.-32-069.03-04910-02-150")
                .manufacturingApproach("протез верхньої кінцівки з повною рухомістю")
                .approvalNumber("02112128")
                .approvalRegistry("60-044-СУПЕРЛЮДИ")
                .approvalSeq("42")
                .build();
    }

    private ProstheticsOrder lowerLimbOrder(String orderNumber) {
        return ProstheticsOrder.builder()
                .orderNumber(orderNumber)
                .prosthesisType("Протез гомілки")
                .productType(ProductType.LOWER_LIMB)
                .patient(ProstheticsPatient.builder()
                        .pib("Тест Пацієнт")
                        .birthDate(LocalDate.of(1990, 1, 1))
                        .build())
                .build();
    }

    @Test
    void upperLimbOrderGeneratesTwoPageTemplateWithAllSections() throws Exception {
        byte[] pdf = service.generateOrderRecipe(upperLimbOrder());

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});

        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
            String text = PdfTextExtractor.getTextFromPage(doc.getPage(1))
                    + PdfTextExtractor.getTextFromPage(doc.getPage(2));

            // Page 1: header, title, sections, personal data
            assertThat(text).contains("ЗАМОВЛЕННЯ № PR-2026-0001");
            assertThat(text).contains("на протези верхніх кінцівок");
            assertThat(text).contains("ЗАТВЕРДЖЕНО");
            assertThat(text).contains("БО «БФ «СУПЕРЛЮДИ»");
            assertThat(text).contains("Загальні відомості про особу");
            assertThat(text).contains("Причина та рівень порушень кінцівки, стан кукси");
            assertThat(text).contains("Загальний стан здоров'я");
            assertThat(text).contains("Діагноз по типу конструкції протезу");
            assertThat(text).contains("Сніжко Іван Петрович");
            assertThat(text).contains("Мінно-вибухова травма");
            assertThat(text).contains("06 18 09.В-ТР.-32-069.03-04910-02-150");
            assertThat(text).contains("Циліндрична");
            assertThat(text).contains("Лікар");
            assertThat(text).contains("Технік");
            assertThat(text).contains("Із призначенням ознайомлений(на)");
            assertThat(text).contains("Дата передання виробу у виробництво");

            // Page 2: materials table + fittings protocol
            assertThat(text).contains("Комплектувальні вироби та матеріали");
            assertThat(text).contains("Силіконовий чохол");
            assertThat(text).contains("Примірки");
            assertThat(text).contains("a/B-TR  видача протезу");
            assertThat(text).contains("Начальник відділу протезування верхніх кінцівок");
        }
    }

    @Test
    void lowerLimbOrderGeneratesSinglePageWithoutFittings() throws Exception {
        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("PR-2026-0002")
                .patient(upperLimbPatient())
                .prosthesisType("Протез гомілки")
                .productType(ProductType.LOWER_LIMB)
                .prescriptionDate(LocalDate.of(2026, 7, 22))
                .build();

        byte[] pdf = service.generateOrderRecipe(order);

        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            String text = PdfTextExtractor.getTextFromPage(doc.getPage(1));
            assertThat(text).contains("ЗАМОВЛЕННЯ № PR-2026-0002");
            assertThat(text).contains("на протез");
            assertThat(text).doesNotContain("Примірки");
        }
    }

    @Test
    void missingOptionalDataRendersPlaceholdersInsteadOfFailing() throws Exception {
        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("PR-2026-0003")
                .productType(ProductType.UPPER_LIMB)
                .build();

        byte[] pdf = service.generateOrderRecipe(order);

        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
            String text = PdfTextExtractor.getTextFromPage(doc.getPage(1));
            assertThat(text).contains("ЗАМОВЛЕННЯ № PR-2026-0003");
        }
    }

    @Test
    void lowerLimbOrderGeneratesFinalReport() throws Exception {
        FlowInstance instance = FlowInstance.builder()
                .status(FlowInstanceStatus.IN_PROGRESS)
                .startTime(LocalDate.of(2026, 8, 31).atTime(10, 0))
                .templateSnapshot("{\"stages\":[]}")
                .build();
        instance.setId(UUID.randomUUID());
        ProstheticsOrder order = lowerLimbOrder("PR-LL-02-0001");
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .name("TP-LL-02")
                .version(1)
                .build();

        byte[] pdf = service.generateFinalReport(instance, order, snapshot, List.of(), List.of(), List.of());

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            String text = PdfTextExtractor.getTextFromPage(doc.getPage(1));
            assertThat(text).contains("ЗВІТ ПРО ВИКОНАННЯ ТЕХНОЛОГІЧНОГО ПРОЦЕСУ");
            assertThat(text).contains("TP-LL-02");
            assertThat(text).contains("PR-LL-02-0001");
            assertThat(text).contains(instance.getId().toString());
            assertThat(text).contains("Тест Пацієнт");
            assertThat(text).contains("IN_PROGRESS");
        }
    }

    @Test
    void failureReport_containsFailReasonAndCategory() throws Exception {
        FlowInstance instance = FlowInstance.builder()
                .status(FlowInstanceStatus.FAILED)
                .startTime(LocalDate.of(2026, 8, 31).atTime(10, 0))
                .templateSnapshot("{\"stages\":[]}")
                .build();
        instance.setId(UUID.randomUUID());
        ProstheticsOrder order = lowerLimbOrder("PR-LL-02-0002");
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .name("TP-LL-02")
                .version(1)
                .build();

        byte[] pdf = service.generateFailureReport(instance, order, snapshot, "MATERIAL_DEFECT", "Гільза тріснула");

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            String text = PdfTextExtractor.getTextFromPage(doc.getPage(1));
            assertThat(text).contains("ЗВІТ ПРО НЕВИКОНАННЯ");
            assertThat(text).contains("MATERIAL_DEFECT");
            assertThat(text).contains("Гільза тріснула");
        }
    }
}
