package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReworkLoopResponse {
    UUID id;
    UUID targetStageId;
    UUID targetStepId;
    String reworkType;
    Integer maxAttempts;
}
