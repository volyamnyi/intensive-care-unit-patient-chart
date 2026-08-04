package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateElementResponse {
    UUID id;
    Integer orderIndex;
    String elementType;
    String label;
    String placeholder;
    Boolean required;
    String unit;
    BigDecimal minValue;
    BigDecimal maxValue;
    Integer minCount;
    Integer maxCount;
    String mimeTypes;
    Integer maxSizeMb;
    String regexPattern;
    String options;
    String validationRules;
}
