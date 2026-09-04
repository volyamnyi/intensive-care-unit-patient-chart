package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateStepResponse {
    UUID id;
    Integer orderIndex;
    String name;
    String description;
    String stepType;
    Boolean mandatory;
    Boolean allowBackward;
    Boolean autoStartTimer;
    Integer normDurationMin;
    List<TemplateElementResponse> elements;
}
