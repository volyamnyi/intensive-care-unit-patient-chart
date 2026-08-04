package com.superhumans.prosthesismanufacturing.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowInstanceValidationTest {

    @Test
    void shouldRejectNegativeActiveSeconds() {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .totalActiveSeconds(-1L)
                .build();

        assertThatThrownBy(instance::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active seconds");
    }

    @Test
    void shouldRejectNegativeReworkCount() {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .reworkCount(-1)
                .build();

        assertThatThrownBy(instance::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rework");
    }

    @Test
    void shouldAcceptNewInstanceWithDefaults() {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .build();

        assertThatCode(instance::validate).doesNotThrowAnyException();
        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.NEW);
        assertThat(instance.getTotalActiveSeconds()).isZero();
    }
}
