package com.superhumans.prosthesismanufacturing.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateElementValidationTest {

    @Test
    void shouldRejectMinGreaterThanMax() {
        TemplateElement element = TemplateElement.builder()
                .orderIndex(0)
                .elementType(ElementType.NUMERIC_INPUT)
                .label("Вага")
                .minValue(new BigDecimal("90.0"))
                .maxValue(new BigDecimal("10.0"))
                .build();

        assertThatThrownBy(element::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Min value");
    }

    @Test
    void shouldRejectMinCountGreaterThanMaxCount() {
        TemplateElement element = TemplateElement.builder()
                .orderIndex(0)
                .elementType(ElementType.FILE_UPLOAD)
                .label("Фото")
                .minCount(3)
                .maxCount(1)
                .build();

        assertThatThrownBy(element::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Min count");
    }

    @Test
    void shouldRejectInvalidMaxSize() {
        TemplateElement element = TemplateElement.builder()
                .orderIndex(0)
                .elementType(ElementType.IMAGE_UPLOAD)
                .label("Фото кукси")
                .maxSizeMb(0)
                .build();

        assertThatThrownBy(element::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max size");
    }

    @Test
    void shouldAcceptValidRanges() {
        TemplateElement element = TemplateElement.builder()
                .orderIndex(0)
                .elementType(ElementType.NUMERIC_INPUT)
                .label("Обхват")
                .minValue(new BigDecimal("10.0"))
                .maxValue(new BigDecimal("60.0"))
                .minCount(1)
                .maxCount(5)
                .maxSizeMb(10)
                .build();

        assertThatCode(element::validate).doesNotThrowAnyException();
    }
}
