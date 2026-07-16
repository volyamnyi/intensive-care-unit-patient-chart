package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClinicalDayCreateRequest {
    @NotNull
    UUID episodeId;
    @NotNull
    LocalDateTime startDateTime;
    @NotNull
    LocalDateTime endDateTime;
}
