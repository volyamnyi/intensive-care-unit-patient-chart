package com.superhumans.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalOrderPatchRequest {
    @Size(max = 100)
    String dose;
    @Size(max = 50)
    String route;
    @Size(max = 100)
    String frequency;
    LocalDateTime endTime;
    @NotNull
    Integer version;
}
