package com.superhumans.prosthesismanufacturing.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReworkLoopValidationTest {

    @Test
    void shouldRejectZeroMaxAttempts() {
        ReworkLoop loop = ReworkLoop.builder()
                .reworkType(ReworkType.FULL)
                .maxAttempts(0)
                .build();

        assertThatThrownBy(loop::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attempts");
    }

    @Test
    void shouldAcceptValidLoop() {
        ReworkLoop loop = ReworkLoop.builder()
                .reworkType(ReworkType.PARTIAL)
                .maxAttempts(2)
                .build();

        assertThatCode(loop::validate).doesNotThrowAnyException();
        assertThat(loop.getMaxAttempts()).isEqualTo(2);
    }
}
