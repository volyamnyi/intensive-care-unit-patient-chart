package com.superhumans.prosthesismanufacturing.dto;

import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplatePatchRequest {
    String description;

    @Positive
    Integer estimatedDurationMin;

    TemplateStatus status;
}
