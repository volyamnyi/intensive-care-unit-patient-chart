package com.superhumans.prosthesismanufacturing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BrakCreateRequest(
        @NotNull UUID returnStageId,
        boolean softTissueMisalignment,
        boolean painDiscomfort,
        @Size(max = 1000, message = "Note must not exceed 1000 characters") String note
) {}
