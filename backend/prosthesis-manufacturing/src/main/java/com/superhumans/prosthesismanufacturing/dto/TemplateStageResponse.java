package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateStageResponse {
    UUID id;
    Integer orderIndex;
    String name;
    String type;
    Boolean canSkip;
    Boolean requiresApproval;
    List<TemplateStepResponse> steps;
    QualityGateResponse gate;
}
