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

    @Test
    void setsRecordHourFromRecordTime() {
        HourlyRecord record = createRecord();
        record.setRecordTime(LocalDateTime.of(2025, 6, 1, 14, 30));
        record.setHeartRate(80);
        record.validateClinicalRanges();
        assertThat(record.getRecordHour()).isEqualTo(14);
    }
}
