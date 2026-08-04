package com.superhumans.prosthesismanufacturing.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowTemplateValidationTest {

    @Test
    void shouldRejectVersionZero() {
        FlowTemplate template = FlowTemplate.builder()
                .name("TP-UL-01")
                .templateVersion(0)
                .productType(ProductType.UPPER_LIMB)
                .build();

        assertThatThrownBy(template::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version");
    }

    @Test
    void shouldAcceptVersionOne() {
        FlowTemplate template = FlowTemplate.builder()
                .name("TP-UL-01")
                .templateVersion(1)
                .productType(ProductType.UPPER_LIMB)
                .build();

        assertThatCode(template::validate).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNegativeEstimatedDuration() {
        FlowTemplate template = FlowTemplate.builder()
                .name("TP-UL-01")
                .templateVersion(1)
                .productType(ProductType.UPPER_LIMB)
                .estimatedDurationMin(-5)
                .build();

        assertThatThrownBy(template::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duration");
    }
}
