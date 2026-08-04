package com.superhumans.prosthesismanufacturing.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateStepValidationTest {

    @Test
    void shouldRejectNegativeOrderIndex() {
        TemplateStep step = TemplateStep.builder()
                .orderIndex(-1)
                .name("Крок")
                .stepType(StepType.INFORMATION)
                .build();

        assertThatThrownBy(step::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("order index");
    }

    @Test
    void shouldRejectNegativeNormDuration() {
        TemplateStep step = TemplateStep.builder()
                .orderIndex(0)
                .name("Крок")
                .stepType(StepType.MEASUREMENT)
                .normDurationMin(-10)
                .build();

        assertThatThrownBy(step::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duration");
    }

    @Test
    void shouldAcceptValidStep() {
        TemplateStep step = TemplateStep.builder()
                .orderIndex(1)
                .name("Зняття мірок")
                .stepType(StepType.MEASUREMENT)
                .normDurationMin(30)
                .build();

        assertThatCode(step::validate).doesNotThrowAnyException();
    }
}
