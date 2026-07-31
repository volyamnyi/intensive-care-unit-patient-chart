package com.superhumans.service.scale;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CamIcuCalculatorTest {

    @Test
    void calculate_allFeaturesFalse_returnsNoDelirium() {
        var input = CamIcuCalculator.CamIcuInput.builder()
                .acuteOnset(false)
                .inattention(false)
                .disorganizedThinking(false)
                .alteredConsciousness(false)
                .build();

        var result = CamIcuCalculator.calculate(input);

        assertThat(result.isDelirium()).isFalse();
    }

    @Test
    void calculate_onlyAcuteOnset_returnsNoDelirium() {
        var input = CamIcuCalculator.CamIcuInput.builder()
                .acuteOnset(true)
                .inattention(false)
                .disorganizedThinking(false)
                .alteredConsciousness(false)
                .build();

        var result = CamIcuCalculator.calculate(input);

        assertThat(result.isDelirium()).isFalse();
    }

    @Test
    void calculate_acuteOnsetAndInattentionOnly_returnsNoDelirium() {
        var input = CamIcuCalculator.CamIcuInput.builder()
                .acuteOnset(true)
                .inattention(true)
                .disorganizedThinking(false)
                .alteredConsciousness(false)
                .build();

        var result = CamIcuCalculator.calculate(input);

        assertThat(result.isDelirium()).isFalse();
    }

    @Test
    void calculate_acuteOnsetAndInattentionWithDisorganizedThinking_returnsDelirium() {
        var input = CamIcuCalculator.CamIcuInput.builder()
                .acuteOnset(true)
                .inattention(true)
                .disorganizedThinking(true)
                .alteredConsciousness(false)
                .build();

        var result = CamIcuCalculator.calculate(input);

        assertThat(result.isDelirium()).isTrue();
    }

    @Test
    void calculate_acuteOnsetAndInattentionWithAlteredConsciousness_returnsDelirium() {
        var input = CamIcuCalculator.CamIcuInput.builder()
                .acuteOnset(true)
                .inattention(true)
                .disorganizedThinking(false)
                .alteredConsciousness(true)
                .build();

        var result = CamIcuCalculator.calculate(input);

        assertThat(result.isDelirium()).isTrue();
    }

    @Test
    void calculate_allFeaturesTrue_returnsDelirium() {
        var input = CamIcuCalculator.CamIcuInput.builder()
                .acuteOnset(true)
                .inattention(true)
                .disorganizedThinking(true)
                .alteredConsciousness(true)
                .build();

        var result = CamIcuCalculator.calculate(input);

        assertThat(result.isDelirium()).isTrue();
    }

    @Test
    void calculate_acuteOnsetAndInattentionWithBothFeature3And4_returnsDelirium() {
        var input = CamIcuCalculator.CamIcuInput.builder()
                .acuteOnset(true)
                .inattention(true)
                .disorganizedThinking(true)
                .alteredConsciousness(true)
                .build();

        var result = CamIcuCalculator.calculate(input);

        assertThat(result.isDelirium()).isTrue();
        assertThat(result.isFeature1()).isTrue();
        assertThat(result.isFeature2()).isTrue();
        assertThat(result.isFeature3()).isTrue();
        assertThat(result.isFeature4()).isTrue();
    }

    @Test
    void calculate_inattentionAndAlteredConsciousnessWithoutAcuteOnset_returnsNoDelirium() {
        var input = CamIcuCalculator.CamIcuInput.builder()
                .acuteOnset(false)
                .inattention(true)
                .disorganizedThinking(false)
                .alteredConsciousness(true)
                .build();

        var result = CamIcuCalculator.calculate(input);

        assertThat(result.isDelirium()).isFalse();
    }

    @Test
    void calculate_acuteOnsetAndAlteredConsciousnessWithoutInattention_returnsNoDelirium() {
        var input = CamIcuCalculator.CamIcuInput.builder()
                .acuteOnset(true)
                .inattention(false)
                .disorganizedThinking(false)
                .alteredConsciousness(true)
                .build();

        var result = CamIcuCalculator.calculate(input);

        assertThat(result.isDelirium()).isFalse();
    }
}
