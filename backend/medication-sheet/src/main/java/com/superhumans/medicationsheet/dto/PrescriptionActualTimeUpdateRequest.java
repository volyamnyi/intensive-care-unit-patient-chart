package com.superhumans.medicationsheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionActualTimeUpdateRequest {
    /** Required actual administration time, ISO local "yyyy-MM-dd'T'HH:mm[:ss]" (#443 V1). */
    @NotBlank @Size(max = 32) String administeredAt;
    /** Optional actual dispensing time, same format; must not be after administeredAt (V4). */
    @Size(max = 32) String dispensedAt;
    /** Mandatory reason for the correction (audit NARRATIVE, appended to execution.comment). */
    @NotBlank @Size(max = 500) String reason;
    /** Optimistic-lock version of the execution; stale value -> 409 VERSION_CONFLICT. */
    @NotNull Integer version;
}
