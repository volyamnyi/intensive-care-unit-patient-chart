package com.superhumans.mis.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AllergyMisDTO {
    Long patientId;
    String allergenName;
    Integer sourceDocumentId;
}
