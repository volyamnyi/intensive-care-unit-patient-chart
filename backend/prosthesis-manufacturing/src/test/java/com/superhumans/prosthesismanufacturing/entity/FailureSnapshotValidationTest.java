package com.superhumans.prosthesismanufacturing.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FailureSnapshotValidationTest {

    @Test
    void shouldRejectBlankCategory() {
        FailureSnapshot snapshot = FailureSnapshot.builder()
                .instance(new FlowInstance())
                .category(" ")
                .build();

        assertThatThrownBy(snapshot::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");
    }

    @Test
    void shouldAcceptValidSnapshot() {
        FailureSnapshot snapshot = FailureSnapshot.builder()
                .instance(new FlowInstance())
                .category("матеріали")
                .description("Недостатній запас термопласту")
                .build();

        assertThatCode(snapshot::validate).doesNotThrowAnyException();
    }
}
