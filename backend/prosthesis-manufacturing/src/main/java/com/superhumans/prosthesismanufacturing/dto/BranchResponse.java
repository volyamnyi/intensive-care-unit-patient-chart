package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BranchResponse {
    UUID brakEventId;
    UUID originalInstanceId;
    UUID newInstanceId;
    UUID returnStageId;
    String returnStageName;
    String newStatus;
}
