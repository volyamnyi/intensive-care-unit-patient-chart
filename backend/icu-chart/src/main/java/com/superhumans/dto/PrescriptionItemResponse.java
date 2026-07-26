package com.superhumans.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

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
