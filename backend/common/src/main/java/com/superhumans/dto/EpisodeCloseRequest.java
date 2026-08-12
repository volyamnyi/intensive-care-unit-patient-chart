package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EpisodeCloseRequest {
    @NotNull
    LocalDateTime dischargeDate;
    @NotNull
    Integer version;
}
