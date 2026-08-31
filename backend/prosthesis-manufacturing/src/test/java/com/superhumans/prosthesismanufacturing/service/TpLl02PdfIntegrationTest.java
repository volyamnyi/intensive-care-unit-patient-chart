package com.superhumans.prosthesismanufacturing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.TemplateCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.TemplatePatchRequest;
import com.superhumans.prosthesismanufacturing.entity.ElementType;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.entity.StageType;
import com.superhumans.prosthesismanufacturing.entity.StepType;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.seed-data.enabled=false")
@Transactional("prosthTransactionManager")
class TpLl02PdfIntegrationTest {

    @Autowired
    private FlowInstanceService instanceService;

    @Autowired
    private FlowTemplateService templateService;

    @Autowired
    private ProstheticsPdfService pdfService;

    @Autowired
    private ProstheticsOrderRepository orderRepository;

    @Autowired
    private ProstheticsPatientRepository prostheticsPatientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void finalReport_containsAllStagesAndSteps() throws Exception {
        UUID templateId = createTpLl02Template();
        UUID instanceId = createAndCompleteInstance(templateId);

        byte[] pdf = instanceService.generateReport(instanceId, 1L, true);

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});

        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            String text = "";
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                text += PdfTextExtractor.getTextFromPage(doc.getPage(i));
            }

            assertThat(text).contains("ЗВІТ ПРО ВИКОНАННЯ ТЕХНОЛОГІЧНОГО ПРОЦЕСУ");
            assertThat(text).contains("TP-LL-02");
            assertThat(text).contains("Тест Пацієнт");
            assertThat(text).contains("IN_PROGRESS");
            assertThat(text).contains("Зняття та внесення об'ємних розмірів");
        }
    }

    @Test
    void failureReport_containsFailReasonAndCategory() throws Exception {
        UUID templateId = createTpLl02Template();
        var order = createTestOrder();
        var instance = instanceService.create(new InstanceCreateRequest(order.getId(), templateId), 1L);
        var started = instanceService.start(instance.getId(), 1L);

        instanceService.fail(started.getId(), "material_defect", "Гільза тріснала", null, 1L);

        byte[] pdf = instanceService.generateReport(started.getId(), 1L, true);

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});

        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            String text = "";
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                text += PdfTextExtractor.getTextFromPage(doc.getPage(i));
            }
            assertThat(text).contains("ЗВІТ ПРО НЕВИКОНАННЯ");
            assertThat(text).contains("material_defect");
            assertThat(text).contains("Гільза тріснала");
        }
    }

    private UUID createTpLl02Template() {
        var request = TemplateCreateRequest.builder()
                .name("TP-LL-02-TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .productType(ProductType.LOWER_LIMB)
                .amputationLevel("generic_lower_limb")
                .limbSide(LimbSide.LEFT)
                .estimatedDurationMin(540)
                .stages(List.of(
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Виготовлення гіпсового негатива")
                                .type(StageType.TECHNICAL)
                                .canSkip(false)
                                .requiresApproval(false)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Зняття та внесення об'ємних розмірів")
                                                .stepType(StepType.MEASUREMENT)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(true)
                                                .normDurationMin(20)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.NUMERIC_INPUT).label("Довжина кукси, см").required(true).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("200")).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.NUMERIC_INPUT).label("Обхват кукси, см").required(true).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("200")).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Гіпсовий негатив виготовлено").required(true).build()))
                                                .build(),
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Виготовлення гіпсового негатива")
                                                .stepType(StepType.INFORMATION)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(false)
                                                .normDurationMin(15)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Гіпсовий негатив виготовлено").required(true).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Виготовлення гіпсової моделі кукси")
                                .type(StageType.TECHNICAL)
                                .canSkip(false)
                                .requiresApproval(false)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Виготовлення гіпсового позитива")
                                                .stepType(StepType.INFORMATION)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(false)
                                                .normDurationMin(15)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Гіпсовий позитив виготовлено").required(true).build()))
                                                .build(),
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Перевірка гіпсового позитива")
                                                .stepType(StepType.CHECKLIST)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(false)
                                                .normDurationMin(10)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Гіпсовий позитив перевірено. Об'ємні та лінійні розміри гіпсової моделі відповідають даним бланку заміру. Поверхня гіпсової моделі повинна бути рівною, гладкою, без дефектів (тріщин, бугрів і тд).").required(true).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Виготовлення тренувальної гільзи")
                                .type(StageType.TECHNICAL)
                                .canSkip(false)
                                .requiresApproval(false)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Виготовлення тренувальної гільзи")
                                                .stepType(StepType.MEASUREMENT)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(true)
                                                .normDurationMin(30)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.NUMERIC_INPUT).label("Довжина гільзи, см").required(true).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("100")).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.NUMERIC_INPUT).label("Обхват гільзи, см").required(true).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("100")).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Тренувальна гільза виготовлена").required(true).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Примірка тренувальної гільзи")
                                .type(StageType.TECHNICAL)
                                .canSkip(false)
                                .requiresApproval(false)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Примірка тренувальної гільзи")
                                                .stepType(StepType.CHECKLIST)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(false)
                                                .normDurationMin(20)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Тренувальна гільза примірена").required(true).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.TEXTAREA).label("Коментар").required(false).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Складання тренувального протеза")
                                .type(StageType.TECHNICAL)
                                .canSkip(false)
                                .requiresApproval(false)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Складання тренувального протеза")
                                                .stepType(StepType.CHECKLIST)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(false)
                                                .normDurationMin(30)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Тренувальний протез складено").required(true).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Примірювання та коректування тренувального протеза")
                                .type(StageType.TECHNICAL)
                                .canSkip(false)
                                .requiresApproval(false)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Примірювання та коректування тренувального протеза")
                                                .stepType(StepType.CHECKLIST)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(false)
                                                .normDurationMin(30)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Тренувальний протез примірено та скориговано").required(true).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.TEXTAREA).label("Коментар").required(false).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Виготовлення пом'якшуючого вкладиша та постійної гільзи")
                                .type(StageType.TECHNICAL)
                                .canSkip(false)
                                .requiresApproval(false)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Виготовлення пом'якшуючого вкладиша")
                                                .stepType(StepType.INFORMATION)
                                                .mandatory(false)
                                                .allowBackward(true)
                                                .autoStartTimer(true)
                                                .normDurationMin(20)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Пом'як Hyundai вкладиш виготовлено").required(true).build()))
                                                .build(),
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Виготовлення постійної гільзи")
                                                .stepType(StepType.MEASUREMENT)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(true)
                                                .normDurationMin(30)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.NUMERIC_INPUT).label("Довжина гільзи, см").required(true).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("100")).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.NUMERIC_INPUT).label("Обхват гільзи, см").required(true).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("100")).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Постійна гільза виготовлена").required(true).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Складання постійного протеза")
                                .type(StageType.TECHNICAL)
                                .canSkip(false)
                                .requiresApproval(false)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Складання постійного протеза")
                                                .stepType(StepType.CHECKLIST)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(false)
                                                .normDurationMin(30)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Постійний протез складено").required(true).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Примірювання та коректування постійного протеза")
                                .type(StageType.TECHNICAL)
                                .canSkip(false)
                                .requiresApproval(false)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Примірювання та коректування постійного протеза")
                                                .stepType(StepType.CHECKLIST)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(false)
                                                .normDurationMin(30)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Постійний протез примірено та скориговано").required(true).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.TEXTAREA).label("Коментар").required(false).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Видача протеза")
                                .type(StageType.ADMINISTRATIVE)
                                .canSkip(false)
                                .requiresApproval(true)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Видача протеза")
                                                .stepType(StepType.CHECKLIST)
                                                .mandatory(true)
                                                .allowBackward(false)
                                                .autoStartTimer(false)
                                                .normDurationMin(15)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("На протез нанесено маркування").required(true).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Супровідна документація оформлена").required(true).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Протез переданий пацієнту для подальшої експлуатації").required(true).build()))
                                                .build()))
                                .build()))
                .build();
        var response = templateService.create(request, 1L);
        templateService.update(response.getId(), TemplatePatchRequest.builder().status(TemplateStatus.ACTIVE).build(), 1L);
        return response.getId();
    }

    private UUID createAndCompleteInstance(UUID templateId) {
        var order = createTestOrder();
        var instance = instanceService.create(new InstanceCreateRequest(order.getId(), templateId), 1L);
        var started = instanceService.start(instance.getId(), 1L);
        return instance.getId();
    }

    private ProstheticsOrder createTestOrder() {
        var patient = ProstheticsPatient.builder()
                .pib("Тест Пацієнт")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("Чоловіча")
                .amputationLevel("generic_lower_limb")
                .build();
        var savedPatient = prostheticsPatientRepository.save(patient);
        var order = ProstheticsOrder.builder()
                .orderNumber("PR-TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .patient(savedPatient)
                .productType(ProductType.LOWER_LIMB)
                .limbSide(LimbSide.LEFT)
                .status(OrderStatus.NEW)
                .build();
        return orderRepository.save(order);
    }
}