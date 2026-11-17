package com.superhumans.medicationsheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionExecuteRequest {
    @NotBlank @Size(max = 100) String actualDose;
    @NotBlank @Size(max = 50) String secondPersonLogin;
    @NotBlank @Size(max = 100) String secondPersonPassword;
    /**
     * Optional actual administration time, ISO local "yyyy-MM-dd'T'HH:mm[:ss]"
     * (#443). Absent/blank → server defaults to now(); any parse failure is a
     * 400 (V1), semantic violations are 422 (V2–V4, validated in the service
     * before the 2P check).
     */
    @Size(max = 32) String administeredAt;
    /** Optional actual dispensing time, same format; must not be after administeredAt (V4). */
    @Size(max = 32) String dispensedAt;
}
