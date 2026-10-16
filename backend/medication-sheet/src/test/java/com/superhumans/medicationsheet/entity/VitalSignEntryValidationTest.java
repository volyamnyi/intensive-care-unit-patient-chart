package com.superhumans.medicationsheet.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Entity-level clinical ranges mirror the DTO ranges (#390). */
class VitalSignEntryValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void inRangeValues_pass() {
        VitalSignEntry entry = entry(36.6, 120, 80, 98, 72, 5);

        assertThat(validator.validate(entry)).isEmpty();
    }

    @Test
    void outOfRangeValues_violate() {
        VitalSignEntry entry = entry(30.0, 20, 10, 10, 400, 11);

        var propertyPaths = validator.validate(entry).stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();

        assertThat(propertyPaths).containsExactlyInAnyOrder(
                "temperature", "systolicBp", "diastolicBp", "spo2", "pulse", "painScore");
    }

    private VitalSignEntry entry(double temperature, int systolicBp, int diastolicBp,
            int spo2, int pulse, int painScore) {
        VitalSignEntry entry = new VitalSignEntry();
        entry.setPeriod("morning");
        entry.setTemperature(temperature);
        entry.setSystolicBp(systolicBp);
        entry.setDiastolicBp(diastolicBp);
        entry.setSpo2(spo2);
        entry.setPulse(pulse);
        entry.setPainScore(painScore);
        return entry;
    }
}
