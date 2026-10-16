package com.superhumans.medicationsheet.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.superhumans.medicationsheet.dto.VitalSignEntryRequest;
import com.superhumans.util.ClinicalConstants;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Static tripwire (#427 item 7): every vital-sign range bound on the DTO and
 * the entity must come from {@link ClinicalConstants} (same source as
 * HourlyRecord) — a literal bound means the two layers can drift on
 * patient-safety-adjacent validation.
 */
class VitalSignRangesTripwireTest {

    @Test
    void rangeBounds_comeFromClinicalConstants() {
        Set<String> allowed = new HashSet<>();
        for (Field constant : ClinicalConstants.class.getDeclaredFields()) {
            if (Modifier.isStatic(constant.getModifiers())) {
                try {
                    allowed.add(String.valueOf(constant.get(null)));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        Set<String> literals = new HashSet<>();
        for (Class<?> type : new Class<?>[]{VitalSignEntryRequest.class, VitalSignEntry.class}) {
            for (Field field : type.getDeclaredFields()) {
                Min min = field.getAnnotation(Min.class);
                if (min != null && !allowed.contains(String.valueOf(min.value()))) {
                    literals.add(type.getSimpleName() + "." + field.getName() + " @Min(" + min.value() + ")");
                }
                Max max = field.getAnnotation(Max.class);
                if (max != null && !allowed.contains(String.valueOf(max.value()))) {
                    literals.add(type.getSimpleName() + "." + field.getName() + " @Max(" + max.value() + ")");
                }
                DecimalMin decimalMin = field.getAnnotation(DecimalMin.class);
                if (decimalMin != null && !allowed.contains(decimalMin.value())) {
                    literals.add(type.getSimpleName() + "." + field.getName() + " @DecimalMin("
                            + decimalMin.value() + ")");
                }
                DecimalMax decimalMax = field.getAnnotation(DecimalMax.class);
                if (decimalMax != null && !allowed.contains(decimalMax.value())) {
                    literals.add(type.getSimpleName() + "." + field.getName() + " @DecimalMax("
                            + decimalMax.value() + ")");
                }
            }
        }
        assertThat(literals)
                .describedAs("vital-sign range literals outside ClinicalConstants")
                .isEmpty();
    }
}
