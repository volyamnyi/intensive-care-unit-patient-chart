package com.superhumans.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionListCreateRequest {
    @NotBlank
    String patientId;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionItemAddRequest {
    @NotBlank String medicineName;
    String medicineMethod;
    String regime;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionDoseRequest {
    @NotBlank String dose;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionExecuteRequest {
    @NotBlank String actualDose;
    boolean requires2pAuth;
    UUID secondPersonId;
}

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionListResponse {
    UUID id;
    Long patientId;
    UUID hospitalizationId;
    Long departmentId;
    String documentName;
    String status;
    UUID editingUserId;
}

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionItemResponse {
    UUID id;
    UUID listId;
    String medicineName;
    String medicineMethod;
    String regime;
    String status;
    Integer sortOrder;
}

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionDayPartResponse {
    UUID id;
    UUID dayId;
    String period;
    String dose;
    Boolean isPlanned;
    Boolean isPlannedFinished;
    Boolean isCompleted;
    Boolean isCompletedFinished;
    String doctorName;
    String nurseName;
}
