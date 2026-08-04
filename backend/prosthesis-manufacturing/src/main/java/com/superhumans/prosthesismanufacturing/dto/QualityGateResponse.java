package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QualityGateResponse {
    UUID id;
    String name;
    String description;
    String requiredApproverRole;
    String checklist;
    Boolean attachmentsRequired;
    List<ReworkLoopResponse> reworkLoops;
}
