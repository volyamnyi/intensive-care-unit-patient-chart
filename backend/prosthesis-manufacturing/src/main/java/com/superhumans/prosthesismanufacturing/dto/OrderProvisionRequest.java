package com.superhumans.prosthesismanufacturing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Materialises a local order from an MIS limb-prosthesis document
 * (setup step 2): order selection never depends on pre-existing local
 * rows — picking an MIS document provisions (find-or-create) the local
 * counterpart the rest of the flow (review → template → instance) runs on.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderProvisionRequest {
    @Pattern(regexp = "\\d+", message = "ID пацієнта має містити лише цифри")
    String patientId;
    @NotNull(message = "ID документа є обов'язковим")
    Long documentId;
}
