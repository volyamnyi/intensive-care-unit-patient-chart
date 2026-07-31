package com.superhumans.service.scale;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApacheIiCalculatorTest {

    @Test
    void calculate_withAllNormalValues_returnsZeroAPS() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .temperatureC(37.0)
                .meanArterialPressure(90.0)
                .heartRate(80.0)
                .respiratoryRate(16.0)
                .fio2(0.21)
                .paO2(95.0)
                .ph(7.40)
                .serumSodium(140.0)
                .serumPotassium(4.0)
                .serumCreatinine(0.9)
                .hematocrit(38.0)
                .whiteBloodCount(8.0)
                .gcs(15)
                .age(35)
                .chronicHealthType("NONE")
                .isEmergencySurgical(false)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isZero();
        assertThat(score.getAgePoints()).isZero();
        assertThat(score.getChronicPoints()).isZero();
        assertThat(score.getTotal()).isZero();
    }

    @Test
    void calculate_temperatureExtremes_givesMaxPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .temperatureC(41.5)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_hypothermia_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .temperatureC(29.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_highHeartRate_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .heartRate(190.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_bradycardia_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .heartRate(35.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_highMAP_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .meanArterialPressure(170.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_lowMAP_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .meanArterialPressure(40.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_highRespiratoryRate_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .respiratoryRate(55.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_lowRespiratoryRate_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .respiratoryRate(4.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_oxygenationHighFiO2WithHighAaDo2_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .fio2(0.8)
                .paO2(55.0)
                .paCO2(40.0)
                .aaDo2(550.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_oxygenationLowFiO2WithLowPaO2_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .fio2(0.3)
                .paO2(45.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_extremePh_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .ph(7.8)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_phFallbackToHco3_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .serumHco3(55.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_highSodium_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .serumSodium(185.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_lowSodium_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .serumSodium(105.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_highPotassium_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .serumPotassium(7.5)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_lowPotassium_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .serumPotassium(2.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_creatinineWithRenalFailure_doublesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .serumCreatinine(4.0)
                .acuteRenalFailure(true)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(8);
    }

    @Test
    void calculate_highHematocrit_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .hematocrit(65.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_lowHematocrit_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .hematocrit(18.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_highWBC_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .whiteBloodCount(45.0)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_lowWBC_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .whiteBloodCount(0.5)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(4);
    }

    @Test
    void calculate_lowGCS_givesMax12Points() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .gcs(3)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isEqualTo(12);
    }

    @Test
    void calculate_highAge_givesPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .age(80)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAgePoints()).isEqualTo(6);
    }

    @Test
    void calculate_chronicHealthEmergencySurgical_givesMaxPoints() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .chronicHealthType("CHRONIC")
                .isEmergencySurgical(true)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getChronicPoints()).isEqualTo(5);
    }

    @Test
    void calculate_allNullFields_returnsZero() {
        var input = ApacheIiCalculator.ApacheIiInput.builder().build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getAps()).isZero();
        assertThat(score.getAgePoints()).isZero();
        assertThat(score.getChronicPoints()).isZero();
        assertThat(score.getTotal()).isZero();
    }

    @Test
    void calculate_fullSevereCase_returnsHighTotal() {
        var input = ApacheIiCalculator.ApacheIiInput.builder()
                .temperatureC(41.0)
                .meanArterialPressure(40.0)
                .heartRate(180.0)
                .respiratoryRate(55.0)
                .fio2(0.21)
                .paO2(45.0)
                .ph(7.1)
                .serumSodium(110.0)
                .serumPotassium(2.0)
                .serumCreatinine(4.0)
                .acuteRenalFailure(true)
                .hematocrit(18.0)
                .whiteBloodCount(45.0)
                .gcs(3)
                .age(80)
                .chronicHealthType("CHRONIC")
                .isEmergencySurgical(true)
                .build();

        var score = ApacheIiCalculator.calculate(input);

        assertThat(score.getTotal()).isGreaterThan(50);
    }
}
