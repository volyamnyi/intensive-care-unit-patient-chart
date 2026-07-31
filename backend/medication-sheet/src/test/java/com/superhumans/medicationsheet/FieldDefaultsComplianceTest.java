package com.superhumans.medicationsheet;

import com.superhumans.medicationsheet.controller.PrescriptionController;
import com.superhumans.medicationsheet.service.PrescriptionExecutionService;
import com.superhumans.medicationsheet.service.PrescriptionItemService;
import com.superhumans.medicationsheet.service.PrescriptionListService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class FieldDefaultsComplianceTest {

    @Test
    void allControllerFieldsArePrivateAndFinal() {
        assertThatFieldsArePrivateAndFinal(PrescriptionController.class);
    }

    @Test
    void allServiceFieldsArePrivateAndFinal() {
        assertThatFieldsArePrivateAndFinal(PrescriptionExecutionService.class);
        assertThatFieldsArePrivateAndFinal(PrescriptionItemService.class);
        assertThatFieldsArePrivateAndFinal(PrescriptionListService.class);
    }

    private static void assertThatFieldsArePrivateAndFinal(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            assertThat(field.getModifiers())
                    .withFailMessage("Field %s in %s is not private final", field.getName(), clazz.getSimpleName())
                    .isEqualTo(Modifier.PRIVATE | Modifier.FINAL);
        }
    }
}
