package com.superhumans.prosthesismanufacturing.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrakEventValidationTest {

    @Test
    void shouldRejectLongNote() {
        BrakEvent event = BrakEvent.builder()
                .instanceId(UUID.randomUUID())
                .stageId(UUID.randomUUID())
                .stepId(UUID.randomUUID())
                .returnStageId(UUID.randomUUID())
                .note("a".repeat(1001))
                .build();
        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1000");
    }

    @Test
    void shouldRejectNullReturnStageId() {
        BrakEvent event = BrakEvent.builder()
                .instanceId(UUID.randomUUID())
                .stageId(UUID.randomUUID())
                .stepId(UUID.randomUUID())
                .returnStageId(null)
                .build();
        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("returnStageId");
    }

    @Test
    void shouldRejectNullInstanceId() {
        BrakEvent event = BrakEvent.builder()
                .stageId(UUID.randomUUID())
                .stepId(UUID.randomUUID())
                .returnStageId(UUID.randomUUID())
                .build();
        assertThatThrownBy(event::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instanceId");
    }

    @Test
    void shouldAcceptValidEvent() {
        BrakEvent event = BrakEvent.builder()
                .instanceId(UUID.randomUUID())
                .stageId(UUID.randomUUID())
                .stepId(UUID.randomUUID())
                .returnStageId(UUID.randomUUID())
                .softTissueMisalignment(true)
                .painDiscomfort(false)
                .note("примітка")
                .build();
        assertThatCode(event::validate).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptNullNote() {
        BrakEvent event = BrakEvent.builder()
                .instanceId(UUID.randomUUID())
                .stageId(UUID.randomUUID())
                .stepId(UUID.randomUUID())
                .returnStageId(UUID.randomUUID())
                .note(null)
                .build();
        assertThatCode(event::validate).doesNotThrowAnyException();
    }
}
