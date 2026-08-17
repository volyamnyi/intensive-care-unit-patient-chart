package com.superhumans.prosthesismanufacturing.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GateDecisionResponse {
    UUID id;
    UUID instanceId;
    UUID gateId;
    String gateName;
    String decision;
    List<String> criteriaConfirmed;
    String comment;
    Long decidedBy;
    LocalDateTime decidedAt;
}
