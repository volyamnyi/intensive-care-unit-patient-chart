package com.superhumans.prosthesismanufacturing.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceUsageValidationTest {

    @Test
    void shouldRejectNegativeQuantity() {
        FlowInstance instance = new FlowInstance();
        ResourceUsage usage = ResourceUsage.builder()
                .instance(instance)
                .material("Термопласт")
                .qty(new BigDecimal("-1.5"))
                .build();

        assertThatThrownBy(usage::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity");
    }

    @Test
    void shouldRejectNegativeMinutes() {
        FlowInstance instance = new FlowInstance();
        ResourceUsage usage = ResourceUsage.builder()
                .instance(instance)
                .material("Формування")
                .minutes(-30)
                .build();

        assertThatThrownBy(usage::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minutes");
    }

    @Test
    void shouldAcceptValidUsage() {
        FlowInstance instance = new FlowInstance();
        ResourceUsage usage = ResourceUsage.builder()
                .instance(instance)
                .material("Термопласт")
                .qty(new BigDecimal("2.0"))
                .unit("кг")
                .minutes(45)
                .build();

        assertThatCode(usage::validate).doesNotThrowAnyException();
    }
}
