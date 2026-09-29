package com.superhumans.prosthesismanufacturing.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the threshold rule (epic #322, issue #324): escalation
 * fires on every brak with order-chain count &gt;= 3, never below.
 */
class BrakThresholdPolicyTest {

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 2})
    void shouldNotify_belowThreshold(long count) {
        assertThat(BrakThresholdPolicy.shouldNotify(count)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(longs = {3, 4, 5, 10})
    void shouldNotify_atAndAboveThreshold(long count) {
        assertThat(BrakThresholdPolicy.shouldNotify(count)).isTrue();
    }

    @Test
    void threshold_isFixedDomainConstant() {
        assertThat(BrakThresholdPolicy.THRESHOLD).isEqualTo(2L);
        assertThat(BrakThresholdPolicy.FIRST_TRIGGER_COUNT).isEqualTo(3L);
    }
}
