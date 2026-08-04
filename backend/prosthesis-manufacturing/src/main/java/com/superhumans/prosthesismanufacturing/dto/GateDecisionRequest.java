package com.superhumans.prosthesismanufacturing.dto;

import com.superhumans.prosthesismanufacturing.entity.GateDecisionType;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GateDecisionRequest {
    @NotNull
    GateDecisionType decision;

    List<String> criteriaConfirmed;

    String comment;
}
