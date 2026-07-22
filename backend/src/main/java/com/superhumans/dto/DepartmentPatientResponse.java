package com.superhumans.dto;

import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.EpisodeStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepartmentPatientResponse {
    UUID id;
    Long patientId;
    String patientName;
    UUID hospitalizationId;
    UUID departmentId;
    LocalDateTime admissionDate;
    LocalDateTime dischargeDate;
    EpisodeStatus status;
    Long attendingDoctorId;
    String attendingDoctorName;

    String ward;
    String bedNumber;
    String admissionDiagnosis;

    ClinicalDayStatus latestDayStatus;
    Integer latestDayNumber;
    Integer daysSinceAdmission;
}