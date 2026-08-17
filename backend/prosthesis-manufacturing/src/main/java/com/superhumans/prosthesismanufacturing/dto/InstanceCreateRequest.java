package com.superhumans.prosthesismanufacturing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstanceCreateRequest {
    @NotNull
    UUID orderId;

    @NotNull
    UUID templateId;
}
