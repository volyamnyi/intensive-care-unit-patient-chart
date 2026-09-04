package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotElement;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateSnapshotParserTest {

    private TemplateSnapshotParser parser;

    @BeforeEach
    void setUp() {
        parser = new TemplateSnapshotParser(new ObjectMapper());
    }

    @Test
    void parse_roundTripsFullSnapshot() {
        UUID stepId = UUID.randomUUID();
        UUID elementId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        SnapshotTemplate template = SnapshotTemplate.builder()
                .name("TP-UL-01")
                .version(3)
                .productType("upper_limb")
                .amputationLevel("transradial")
                .limbSide("left")
                .estimatedDurationMin(120)
                .stages(List.of(SnapshotStage.builder()
                        .id(stageId)
                        .name("Підготовка")
                        .stageType("preparation")
                        .canSkip(false)
                        .requiresApproval(false)
                        .steps(List.of(SnapshotStep.builder()
                                .id(stepId)
                                .name("Зняття мірки")
                                .stepType("measurement")
                                .mandatory(true)
                                .allowBackward(true)
                                .autoStartTimer(false)
                                .normDurationMin(45)
                                .elements(List.of(SnapshotElement.builder()
                                        .id(elementId)
                                        .elementType("number")
                                        .label("Обхват")
                                        .required(true)
                                        .unit("cm")
                                        .minValue(new BigDecimal("10"))
                                        .maxValue(new BigDecimal("60"))
                                        .minCount(1)
                                        .maxCount(2)
                                        .regexPattern("[0-9]+")
                                        .options(List.of("A", "B"))
                                        .mimeTypes(List.of("image/png"))
                                        .maxSizeMb(10)
                                        .build()))
                                .build()))
                        .build()))
                .build();

        String json = parser.toJson(template);
        SnapshotTemplate parsed = parser.parse(json);

        assertThat(parsed.getName()).isEqualTo("TP-UL-01");
        assertThat(parsed.getVersion()).isEqualTo(3);
        assertThat(parsed.getProductType()).isEqualTo("upper_limb");
        assertThat(parsed.getAmputationLevel()).isEqualTo("transradial");
        assertThat(parsed.getLimbSide()).isEqualTo("left");
        assertThat(parsed.getEstimatedDurationMin()).isEqualTo(120);
        assertThat(parsed.getStages()).hasSize(1);

        SnapshotStage stage = parsed.getStages().get(0);
        assertThat(stage.getId()).isEqualTo(stageId);
        assertThat(stage.getStageType()).isEqualTo("preparation");
        assertThat(stage.isCanSkip()).isFalse();

        SnapshotStep step = stage.getSteps().get(0);
        assertThat(step.getId()).isEqualTo(stepId);
        assertThat(step.isMandatory()).isTrue();
        assertThat(step.isAllowBackward()).isTrue();
        assertThat(step.getNormDurationMin()).isEqualTo(45);

        SnapshotElement element = step.getElements().get(0);
        assertThat(element.getId()).isEqualTo(elementId);
        assertThat(element.getLabel()).isEqualTo("Обхват");
        assertThat(element.getUnit()).isEqualTo("cm");
        assertThat(element.getMinValue()).isEqualByComparingTo("10");
        assertThat(element.getMaxValue()).isEqualByComparingTo("60");
        assertThat(element.getMinCount()).isEqualTo(1);
        assertThat(element.getMaxCount()).isEqualTo(2);
        assertThat(element.getRegexPattern()).isEqualTo("[0-9]+");
        assertThat(element.getOptions()).containsExactly("A", "B");
        assertThat(element.getMimeTypes()).containsExactly("image/png");
        assertThat(element.getMaxSizeMb()).isEqualTo(10);
    }

    @Test
    void parse_minimalSnapshot_roundTrips() {
        SnapshotTemplate template = SnapshotTemplate.builder()
                .name("TP-LL-01")
                .build();

        SnapshotTemplate parsed = parser.parse(parser.toJson(template));

        assertThat(parsed.getName()).isEqualTo("TP-LL-01");
        assertThat(parsed.getStages()).isNull();
    }

    @Test
    void parse_emptyStagesList_roundTrips() {
        SnapshotTemplate template = SnapshotTemplate.builder()
                .name("TP-UL-02")
                .version(1)
                .stages(List.of())
                .build();

        SnapshotTemplate parsed = parser.parse(parser.toJson(template));

        assertThat(parsed.getStages()).isEmpty();
    }

    @Test
    void parse_legacyGateBlock_isIgnored() {
        String legacyJson = "{\"name\":\"TP-UL-01\",\"version\":1,\"stages\":[{\"id\":\"" + UUID.randomUUID()
                + "\",\"name\":\"Підготовка\",\"gate\":{\"id\":\"" + UUID.randomUUID()
                + "\",\"name\":\"Контроль якості\",\"reworkLoops\":[]},\"steps\":[]}]}";

        SnapshotTemplate parsed = parser.parse(legacyJson);

        assertThat(parsed.getStages()).hasSize(1);
        assertThat(parsed.getStages().get(0).getName()).isEqualTo("Підготовка");
    }

    @Test
    void parse_corruptedJson_throws() {
        assertThatThrownBy(() -> parser.parse("{not-valid-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template snapshot is corrupted");
    }

    @Test
    void toJson_nullStages_serializesWithoutError() {
        SnapshotTemplate template = SnapshotTemplate.builder()
                .name("TP-UL-03")
                .build();

        String json = parser.toJson(template);

        assertThat(json).contains("TP-UL-03");
    }
}