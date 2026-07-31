package com.superhumans.service.scale;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BradenCalculatorTest {

    @Test
    void calculate_maxValues_returnsLowRisk() {
        var input = BradenCalculator.BradenInput.builder()
                .sensoryPerception(4)
                .moisture(4)
                .activity(4)
                .mobility(4)
                .nutrition(4)
                .frictionShear(3)
                .build();

        var result = BradenCalculator.calculate(input);

        assertThat(result.getTotal()).isEqualTo(23);
        assertThat(result.getRiskCategory()).isEqualTo("Low");
    }

    @Test
    void calculate_minValues_returnsVeryHighRisk() {
        var input = BradenCalculator.BradenInput.builder()
                .sensoryPerception(1)
                .moisture(1)
                .activity(1)
                .mobility(1)
                .nutrition(1)
                .frictionShear(1)
                .build();

        var result = BradenCalculator.calculate(input);

        assertThat(result.getTotal()).isEqualTo(6);
        assertThat(result.getRiskCategory()).isEqualTo("VeryHigh");
    }

    @Test
    void calculate_midValues_returnsMildRisk() {
        var input = BradenCalculator.BradenInput.builder()
                .sensoryPerception(3)
                .moisture(3)
                .activity(2)
                .mobility(3)
                .nutrition(3)
                .frictionShear(3)
                .build();

        var result = BradenCalculator.calculate(input);

        assertThat(result.getTotal()).isEqualTo(17);
        assertThat(result.getRiskCategory()).isEqualTo("Mild");
    }

    @Test
    void calculate_boundaryLowRisk_returnsLow() {
        var input = BradenCalculator.BradenInput.builder()
                .sensoryPerception(4)
                .moisture(3)
                .activity(3)
                .mobility(3)
                .nutrition(3)
                .frictionShear(3)
                .build();

        var result = BradenCalculator.calculate(input);

        assertThat(result.getTotal()).isEqualTo(19);
        assertThat(result.getRiskCategory()).isEqualTo("Low");
    }

    @Test
    void calculate_boundaryMildRisk_returnsMild() {
        var input = BradenCalculator.BradenInput.builder()
                .sensoryPerception(3)
                .moisture(3)
                .activity(2)
                .mobility(3)
                .nutrition(2)
                .frictionShear(2)
                .build();

        var result = BradenCalculator.calculate(input);

        assertThat(result.getTotal()).isEqualTo(15);
        assertThat(result.getRiskCategory()).isEqualTo("Mild");
    }

    @Test
    void calculate_boundaryModerateRisk_returnsModerate() {
        var input = BradenCalculator.BradenInput.builder()
                .sensoryPerception(2)
                .moisture(2)
                .activity(2)
                .mobility(2)
                .nutrition(3)
                .frictionShear(2)
                .build();

        var result = BradenCalculator.calculate(input);

        assertThat(result.getTotal()).isEqualTo(13);
        assertThat(result.getRiskCategory()).isEqualTo("Moderate");
    }

    @Test
    void calculate_boundaryHighRisk_returnsHigh() {
        var input = BradenCalculator.BradenInput.builder()
                .sensoryPerception(2)
                .moisture(2)
                .activity(1)
                .mobility(2)
                .nutrition(2)
                .frictionShear(1)
                .build();

        var result = BradenCalculator.calculate(input);

        assertThat(result.getTotal()).isEqualTo(10);
        assertThat(result.getRiskCategory()).isEqualTo("High");
    }

    @Test
    void calculate_total9_returnsVeryHigh() {
        var input = BradenCalculator.BradenInput.builder()
                .sensoryPerception(1)
                .moisture(2)
                .activity(1)
                .mobility(2)
                .nutrition(2)
                .frictionShear(1)
                .build();

        var result = BradenCalculator.calculate(input);

        assertThat(result.getTotal()).isEqualTo(9);
        assertThat(result.getRiskCategory()).isEqualTo("VeryHigh");
    }

    @Test
    void calculate_allDefaults_returnsModerateRisk() {
        var input = BradenCalculator.BradenInput.builder()
                .sensoryPerception(3)
                .moisture(3)
                .activity(2)
                .mobility(3)
                .nutrition(3)
                .frictionShear(3)
                .build();

        var result = BradenCalculator.calculate(input);

        assertThat(result.getTotal()).isEqualTo(17);
        assertThat(result.getRiskCategory()).isEqualTo("Mild");
    }
}
