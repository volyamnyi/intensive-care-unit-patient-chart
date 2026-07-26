package com.superhumans.medicationsheet.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AllergyResponse {
    String id;
    Long patientId;
    String allergenName;
    Integer sourceDocumentId;
}
