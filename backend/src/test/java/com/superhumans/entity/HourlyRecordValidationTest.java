package com.superhumans.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

class HourlyRecordValidationTest {

    private HourlyRecord createRecord() {
        HourlyRecord record = new HourlyRecord();
        record.setRecordTime(LocalDateTime.of(2025, 6, 1, 8, 0));
        return record;
    }

    @Test
    void validRecord_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setHeartRate(80);
        record.setSystolicBP(120);
        record.setDiastolicBP(80);
        record.setTemperature(36.6);
        record.setSpo2(98.0);
        record.setRespiratoryRate(16);
        record.setGlucose(5.5);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    @Test
    void nullFields_doesNotThrow() {
        HourlyRecord record = createRecord();
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    @Test
    void heartRate_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setHeartRate(-1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Heart rate");
    }

    @Test
    void heartRate_aboveMax_throws() {
        HourlyRecord record = createRecord();
        record.setHeartRate(301);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Heart rate");
    }

    @Test
    void heartRate_atBoundaries_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setHeartRate(0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
        record.setHeartRate(300);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    @Test
    void systolicBP_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setSystolicBP(49);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Systolic BP");
    }

    @Test
    void systolicBP_aboveMax_throws() {
        HourlyRecord record = createRecord();
        record.setSystolicBP(251);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Systolic BP");
    }

    @Test
    void diastolicBP_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setDiastolicBP(29);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Diastolic BP");
    }

    @Test
    void diastolicBP_aboveMax_throws() {
        HourlyRecord record = createRecord();
        record.setDiastolicBP(151);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Diastolic BP");
    }

    @Test
    void temperature_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setTemperature(33.9);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Temperature");
    }

    @Test
    void temperature_aboveMax_throws() {
        HourlyRecord record = createRecord();
        record.setTemperature(42.1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Temperature");
    }

    @Test
    void spo2_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setSpo2(49.9);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Oxygen saturation");
    }

    @Test
    void spo2_aboveMax_throws() {
        HourlyRecord record = createRecord();
        record.setSpo2(100.1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Oxygen saturation");
    }

    @Test
    void respiratoryRate_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setRespiratoryRate(-1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Respiratory rate");
    }

    @Test
    void respiratoryRate_aboveMax_throws() {
        HourlyRecord record = createRecord();
        record.setRespiratoryRate(61);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Respiratory rate");
    }

    @Test
    void glucose_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setGlucose(0.9);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Glucose");
    }

    @Test
    void glucose_aboveMax_throws() {
        HourlyRecord record = createRecord();
        record.setGlucose(30.1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Glucose");
    }

    // ========== urineOutput ==========

    @Test
    void urineOutput_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setUrineOutput(-0.1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Urine output");
    }

    @Test
    void urineOutput_atMin_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setUrineOutput(0.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    @Test
    void urineOutput_positive_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setUrineOutput(150.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    // ========== drainOutput ==========

    @Test
    void drainOutput_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setDrainOutput(-0.1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Drain output");
    }

    @Test
    void drainOutput_atMin_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setDrainOutput(0.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    @Test
    void drainOutput_positive_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setDrainOutput(75.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    // ========== painScore ==========

    @Test
    void painScore_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setPainScore(-1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pain score");
    }

    @Test
    void painScore_aboveMax_throws() {
        HourlyRecord record = createRecord();
        record.setPainScore(11);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pain score");
    }

    @Test
    void painScore_atBoundaries_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setPainScore(0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
        record.setPainScore(10);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    // ========== etco2 ==========

    @Test
    void etco2_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setEtco2(-0.1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ETCO2");
    }

    @Test
    void etco2_aboveMax_throws() {
        HourlyRecord record = createRecord();
        record.setEtco2(100.1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ETCO2");
    }

    @Test
    void etco2_atBoundaries_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setEtco2(0.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
        record.setEtco2(100.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    @Test
    void etco2_valid_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setEtco2(38.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    // ========== fio2 ==========

    @Test
    void fio2_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setFio2(-0.1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FiO2");
    }

    @Test
    void fio2_aboveMax_throws() {
        HourlyRecord record = createRecord();
        record.setFio2(1.1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FiO2");
    }

    @Test
    void fio2_atBoundaries_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setFio2(0.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
        record.setFio2(1.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    @Test
    void fio2_valid_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setFio2(0.4);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    // ========== cvp ==========

    @Test
    void cvp_belowMin_throws() {
        HourlyRecord record = createRecord();
        record.setCvp(-0.1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CVP");
    }

    @Test
    void cvp_aboveMax_throws() {
        HourlyRecord record = createRecord();
        record.setCvp(30.1);
        assertThatThrownBy(record::validateClinicalRanges)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CVP");
    }

    @Test
    void cvp_atBoundaries_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setCvp(0.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
        record.setCvp(30.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    @Test
    void cvp_valid_doesNotThrow() {
        HourlyRecord record = createRecord();
        record.setCvp(8.0);
        assertThatCode(record::validateClinicalRanges).doesNotThrowAnyException();
    }

    @Test
    void setsRecordHourFromRecordTime() {
        HourlyRecord record = createRecord();
        record.setRecordTime(LocalDateTime.of(2025, 6, 1, 14, 30));
        record.setHeartRate(80);
        record.validateClinicalRanges();
        assertThat(record.getRecordHour()).isEqualTo(14);
    }
}
