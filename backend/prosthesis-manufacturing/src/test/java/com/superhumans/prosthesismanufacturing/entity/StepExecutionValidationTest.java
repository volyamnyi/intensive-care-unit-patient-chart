package com.superhumans.prosthesismanufacturing.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StepExecutionValidationTest {

    @Test
    void shouldRejectZeroAttemptNumber() {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .build();
        StepExecution execution = StepExecution.builder()
                .instance(instance)
                .stageId(UUID.randomUUID())
                .stepId(UUID.randomUUID())
                .attemptNumber(0)
                .build();

        assertThatThrownBy(execution::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attempt");
    }

    @Test
    void shouldRejectNegativeActiveSeconds() {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .build();
        StepExecution execution = StepExecution.builder()
                .instance(instance)
                .stageId(UUID.randomUUID())
                .stepId(UUID.randomUUID())
                .attemptNumber(1)
                .activeSeconds(-5L)
                .build();

        assertThatThrownBy(execution::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active seconds");
    }

    @Test
    void shouldAcceptExecutionWithDefaults() {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .build();
        StepExecution execution = StepExecution.builder()
                .instance(instance)
                .stageId(UUID.randomUUID())
                .stepId(UUID.randomUUID())
                .build();

        assertThatCode(execution::validate).doesNotThrowAnyException();
        assertThat(execution.getStatus()).isEqualTo(StepExecutionStatus.NOT_STARTED);
        assertThat(execution.getAttemptNumber()).isEqualTo(1);
    }
}
