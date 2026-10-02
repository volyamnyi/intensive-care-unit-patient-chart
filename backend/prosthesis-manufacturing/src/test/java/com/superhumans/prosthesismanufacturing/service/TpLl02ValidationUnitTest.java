package com.superhumans.prosthesismanufacturing.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotElement;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Фаза 2 — Business Rules & State Machine (без Quality Gate).
 * Unit-перевірка валідації TP-LL-02: умовний 7.1, numeric range, backward target.
 * The lower-limb measurement step (e0000020) skips the ≥3-values MEASUREMENT
 * threshold — it can advance with any (including zero) filled values.
 */
class TpLl02ValidationUnitTest {

    private FlowInstanceService service;

    @BeforeEach
    void setUp() {
        // Only validateValues is needed — other deps can be null for this test
        service = new FlowInstanceService(null, null, null, null, null, null, null, null, null, null, null, null, new ObjectMapper());
    }

    private SnapshotStep measurementStep() {
        return SnapshotStep.builder()
                .id(UUID.fromString("e0000020-0000-0000-0000-000000000020"))
                .name("Зняття та внесення об'ємних розмірів")
                .stepType("MEASUREMENT")
                .mandatory(true)
                .allowBackward(true)
                .autoStartTimer(true)
                .normDurationMin(20)
                .elements(List.of(
                        SnapshotElement.builder().id(UUID.fromString("f0000200-0000-0000-0000-000000000200")).label("Довжина кукси, см").elementType("NUMERIC_INPUT").required(true).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("200")).build(),
                        SnapshotElement.builder().id(UUID.fromString("f0000201-0000-0000-0000-000000000201")).label("Обхват кукси, см").elementType("NUMERIC_INPUT").required(true).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("200")).build(),
                        SnapshotElement.builder().id(UUID.fromString("f0000202-0000-0000-0000-000000000202")).label("Обхват на рівні 5 см від кукси, см").elementType("NUMERIC_INPUT").required(false).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("200")).build(),
                        SnapshotElement.builder().id(UUID.fromString("f0000203-0000-0000-0000-000000000203")).label("Обхват на рівні 10 см, см").elementType("NUMERIC_INPUT").required(false).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("200")).build(),
                        SnapshotElement.builder().id(UUID.fromString("f0000204-0000-0000-0000-000000000204")).label("Гіпсовий негатив виготовлено").elementType("CHECKBOX").required(true).build()))
                .build();
    }

    private SnapshotStep conditionalInsertStep() {
        return SnapshotStep.builder()
                .id(UUID.fromString("e0000029-0000-0000-0000-000000000029"))
                .name("Виготовлення пом'якшуючого вкладиша")
                .stepType("CHECKLIST")
                .mandatory(false)
                .allowBackward(true)
                .elements(List.of(
                        SnapshotElement.builder().id(UUID.fromString("f0000214-0000-0000-0000-000000000214")).label("Візуальний контроль чистоти пом'якшуючого вкладиша").elementType("CHECKBOX").required(false).build(),
                        SnapshotElement.builder().id(UUID.fromString("f0000215-0000-0000-0000-000000000215")).label("Тактильний контроль пом'якшуючого вкладиша").elementType("CHECKBOX").required(false).build(),
                        SnapshotElement.builder().id(UUID.fromString("f0000240-0000-0000-0000-000000000240")).label("Помʼякшуючий вкладиш не потрібен").elementType("CHECKBOX").required(false).build()))
                .build();
    }

    private SnapshotStep permanentSocketStep() {
        return SnapshotStep.builder()
                .id(UUID.fromString("e0000030-0000-0000-0000-000000000030"))
                .name("Виготовлення постійної гільзи")
                .stepType("CHECKLIST")
                .mandatory(true)
                .elements(List.of(
                        SnapshotElement.builder().id(UUID.fromString("f0000216-0000-0000-0000-000000000216")).label("Візуальний контроль чистоти постійної гільзи").elementType("CHECKBOX").required(true).build(),
                        SnapshotElement.builder().id(UUID.fromString("f0000217-0000-0000-0000-000000000217")).label("Тактильний контроль постійної гільзи").elementType("CHECKBOX").required(true).build()))
                .build();
    }

    @Test
    void lowerLimbMeasurementStep_insufficientValuesPasses() {
        // The lower-limb measurement step (e0000020) intentionally allows advancing
        // without filling any measurement values, so <3 values no longer fails.
        SnapshotStep step = measurementStep();
        String values = """
                {"f0000200-0000-0000-0000-000000000200": "18",
                 "f0000201-0000-0000-0000-000000000201": "24",
                 "f0000204-0000-0000-0000-000000000204": true}
                """;
        assertThatCode(() -> service.validateValues(values, step)).doesNotThrowAnyException();
    }

    @Test
    void measurementStep_threeValuesPasses() {
        SnapshotStep step = measurementStep();
        String values = """
                {"f0000200-0000-0000-0000-000000000200": "18",
                 "f0000201-0000-0000-0000-000000000201": "24",
                 "f0000202-0000-0000-0000-000000000202": "20",
                 "f0000204-0000-0000-0000-000000000204": true}
                """;
        assertThatCode(() -> service.validateValues(values, step)).doesNotThrowAnyException();
    }

    // Phase 3: exclusive soft-liner rule — 8 combos (ALLOW = (v&&t&&!n) || (n&&!v&&!t))
    @Test
    void conditionalInsertStep_emptyValuesDenied() {
        SnapshotStep step = conditionalInsertStep();
        String values = "{}"; // false/false/false -> DENY
        assertThatThrownBy(() -> service.validateValues(values, step))
                .isInstanceOf(com.superhumans.exception.BadRequestException.class);
    }

    @Test
    void conditionalInsertStep_bothControlsWithoutNotRequiredAllowed() {
        SnapshotStep step = conditionalInsertStep();
        String values = """
                {"f0000214-0000-0000-0000-000000000214": true,
                 "f0000215-0000-0000-0000-000000000215": true,
                 "f0000240-0000-0000-0000-000000000240": false}
                """;
        assertThatCode(() -> service.validateValues(values, step)).doesNotThrowAnyException();
    }

    @Test
    void conditionalInsertStep_notRequiredAloneAllowed() {
        SnapshotStep step = conditionalInsertStep();
        String values = """
                {"f0000214-0000-0000-0000-000000000214": false,
                 "f0000215-0000-0000-0000-000000000215": false,
                 "f0000240-0000-0000-0000-000000000240": true}
                """;
        assertThatCode(() -> service.validateValues(values, step)).doesNotThrowAnyException();
    }

    @Test
    void conditionalInsertStep_mixedCombosDenied() {
        SnapshotStep step = conditionalInsertStep();
        // true/false/false -> DENY
        assertThatThrownBy(() -> service.validateValues(
                "{\"f0000214-0000-0000-0000-000000000214\": true, \"f0000215-0000-0000-0000-000000000215\": false, \"f0000240-0000-0000-0000-000000000240\": false}", step))
                .isInstanceOf(com.superhumans.exception.BadRequestException.class);
        // false/true/false -> DENY
        assertThatThrownBy(() -> service.validateValues(
                "{\"f0000214-0000-0000-0000-000000000214\": false, \"f0000215-0000-0000-0000-000000000215\": true, \"f0000240-0000-0000-0000-000000000240\": false}", step))
                .isInstanceOf(com.superhumans.exception.BadRequestException.class);
        // true/true/true -> DENY
        assertThatThrownBy(() -> service.validateValues(
                "{\"f0000214-0000-0000-0000-000000000214\": true, \"f0000215-0000-0000-0000-000000000215\": true, \"f0000240-0000-0000-0000-000000000240\": true}", step))
                .isInstanceOf(com.superhumans.exception.BadRequestException.class);
        // true/false/true -> DENY
        assertThatThrownBy(() -> service.validateValues(
                "{\"f0000214-0000-0000-0000-000000000214\": true, \"f0000215-0000-0000-0000-000000000215\": false, \"f0000240-0000-0000-0000-000000000240\": true}", step))
                .isInstanceOf(com.superhumans.exception.BadRequestException.class);
        // false/true/true -> DENY
        assertThatThrownBy(() -> service.validateValues(
                "{\"f0000214-0000-0000-0000-000000000214\": false, \"f0000215-0000-0000-0000-000000000215\": true, \"f0000240-0000-0000-0000-000000000240\": true}", step))
                .isInstanceOf(com.superhumans.exception.BadRequestException.class);
    }

    @Test
    void permanentSocketStep_emptyFails() {
        SnapshotStep step = permanentSocketStep();
        String values = "{}";
        assertThatThrownBy(() -> service.validateValues(values, step))
                .isInstanceOf(com.superhumans.exception.BadRequestException.class)
                .hasMessageContaining("обов'язкове");
    }

    @Test
    void numericRange_outOfBoundsFails() {
        // For MEASUREMENT numeric range is not validated — use CHECKLIST to test range
        SnapshotStep step = SnapshotStep.builder()
                .id(UUID.randomUUID())
                .name("Контроль числового поля")
                .stepType("CHECKLIST")
                .mandatory(true)
                .elements(List.of(
                        SnapshotElement.builder()
                                .id(UUID.fromString("f0000999-0000-0000-0000-000000000999"))
                                .label("Довжина кукси, см")
                                .elementType("NUMERIC_INPUT")
                                .required(true)
                                .unit("см")
                                .minValue(new BigDecimal("0"))
                                .maxValue(new BigDecimal("200"))
                                .build()))
                .build();
        String values = """
                {"f0000999-0000-0000-0000-000000000999": "300"}
                """;
        assertThatThrownBy(() -> service.validateValues(values, step))
                .isInstanceOf(com.superhumans.exception.BadRequestException.class)
                .hasMessageContaining("не більше");
    }

    @Test
    void numericRange_withinBoundsPasses() {
        SnapshotStep step = SnapshotStep.builder()
                .id(UUID.randomUUID())
                .name("Контроль числового поля")
                .stepType("CHECKLIST")
                .mandatory(true)
                .elements(List.of(
                        SnapshotElement.builder()
                                .id(UUID.fromString("f0000999-0000-0000-0000-000000000999"))
                                .label("Довжина кукси, см")
                                .elementType("NUMERIC_INPUT")
                                .required(true)
                                .unit("см")
                                .minValue(new BigDecimal("0"))
                                .maxValue(new BigDecimal("200"))
                                .build()))
                .build();
        String values = """
                {"f0000999-0000-0000-0000-000000000999": "150"}
                """;
        assertThatCode(() -> service.validateValues(values, step)).doesNotThrowAnyException();
    }
}
