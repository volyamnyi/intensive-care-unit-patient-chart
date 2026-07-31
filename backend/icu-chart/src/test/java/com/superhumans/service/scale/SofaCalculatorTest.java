package com.superhumans.service.scale;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SofaCalculatorTest {

    @Test
    void calculate_allNormalValues_returnsZero() {
        var input = SofaCalculator.SofaInput.builder()
                .paO2(400.0)
                .fio2(21.0)
                .platelets(200.0)
                .bilirubin(0.8)
                .map(80.0)
                .gcs(15)
                .creatinine(0.8)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getTotal()).isZero();
    }

    @Test
    void calculate_severeRespiration_onVentilatorLowRatio() {
        var input = SofaCalculator.SofaInput.builder()
                .paO2(80.0)
                .fio2(50.0)
                .onVentilator(true)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getRespiration()).isEqualTo(3);
    }

    @Test
    void calculate_severeRespiration_onVentilatorVeryLowRatio() {
        var input = SofaCalculator.SofaInput.builder()
                .paO2(50.0)
                .fio2(60.0)
                .onVentilator(true)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getRespiration()).isEqualTo(4);
    }

    @Test
    void calculate_respiration_notOnVentilatorLowRatio_returnsZero() {
        var input = SofaCalculator.SofaInput.builder()
                .paO2(50.0)
                .fio2(60.0)
                .onVentilator(false)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getRespiration()).isZero();
    }

    @Test
    void calculate_severeCoagulation() {
        var input = SofaCalculator.SofaInput.builder()
                .platelets(15.0)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getCoagulation()).isEqualTo(4);
    }

    @Test
    void calculate_moderateCoagulation() {
        var input = SofaCalculator.SofaInput.builder()
                .platelets(75.0)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getCoagulation()).isEqualTo(2);
    }

    @Test
    void calculate_severeLiver() {
        var input = SofaCalculator.SofaInput.builder()
                .bilirubin(12.5)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getLiver()).isEqualTo(4);
    }

    @Test
    void calculate_mildLiver() {
        var input = SofaCalculator.SofaInput.builder()
                .bilirubin(1.5)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getLiver()).isEqualTo(1);
    }

    @Test
    void calculate_severeCardiovascular_highDopamine() {
        var input = SofaCalculator.SofaInput.builder()
                .dopamine(20.0)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getCardiovascular()).isEqualTo(4);
    }

    @Test
    void calculate_severeCardiovascular_highNorepinephrine() {
        var input = SofaCalculator.SofaInput.builder()
                .norepinephrine(0.5)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getCardiovascular()).isEqualTo(4);
    }

    @Test
    void calculate_cardiovascular_epinephrineLow_returnsThree() {
        var input = SofaCalculator.SofaInput.builder()
                .epinephrine(0.1)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getCardiovascular()).isEqualTo(3);
    }

    @Test
    void calculate_cardiovascular_epinephrineHigh_returnsFour() {
        var input = SofaCalculator.SofaInput.builder()
                .epinephrine(0.2)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getCardiovascular()).isEqualTo(4);
    }

    @Test
    void calculate_moderateCardiovascular_dobutamine() {
        var input = SofaCalculator.SofaInput.builder()
                .dobutamine(5.0)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getCardiovascular()).isEqualTo(2);
    }

    @Test
    void calculate_mildCardiovascular_lowMAP() {
        var input = SofaCalculator.SofaInput.builder()
                .map(65.0)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getCardiovascular()).isEqualTo(1);
    }

    @Test
    void calculate_severeCNS() {
        var input = SofaCalculator.SofaInput.builder()
                .gcs(3)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getCns()).isEqualTo(4);
    }

    @Test
    void calculate_mildCNS() {
        var input = SofaCalculator.SofaInput.builder()
                .gcs(14)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getCns()).isEqualTo(1);
    }

    @Test
    void calculate_severeRenal_lowUrineOutput() {
        var input = SofaCalculator.SofaInput.builder()
                .urineOutput(100.0)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getRenal()).isEqualTo(4);
    }

    @Test
    void calculate_severeRenal_highCreatinine() {
        var input = SofaCalculator.SofaInput.builder()
                .creatinine(6.0)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getRenal()).isEqualTo(4);
    }

    @Test
    void calculate_moderateRenal() {
        var input = SofaCalculator.SofaInput.builder()
                .creatinine(3.5)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getRenal()).isEqualTo(3);
    }

    @Test
    void calculate_mildRenal() {
        var input = SofaCalculator.SofaInput.builder()
                .creatinine(2.0)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getRenal()).isEqualTo(2);
    }

    @Test
    void calculate_allNullFields_returnsZero() {
        var input = SofaCalculator.SofaInput.builder().build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getTotal()).isZero();
    }

    @Test
    void calculate_maximalSeverity_returnsMaxTotal() {
        var input = SofaCalculator.SofaInput.builder()
                .paO2(50.0)
                .fio2(60.0)
                .onVentilator(true)
                .platelets(15.0)
                .bilirubin(12.5)
                .dopamine(20.0)
                .gcs(3)
                .urineOutput(100.0)
                .build();

        var score = SofaCalculator.calculate(input);

        assertThat(score.getRespiration()).isEqualTo(4);
        assertThat(score.getCoagulation()).isEqualTo(4);
        assertThat(score.getLiver()).isEqualTo(4);
        assertThat(score.getCardiovascular()).isEqualTo(4);
        assertThat(score.getCns()).isEqualTo(4);
        assertThat(score.getRenal()).isEqualTo(4);
        assertThat(score.getTotal()).isEqualTo(24);
    }
}
