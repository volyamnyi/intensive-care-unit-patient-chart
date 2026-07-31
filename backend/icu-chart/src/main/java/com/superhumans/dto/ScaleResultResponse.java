package com.superhumans.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScaleResultResponse {
    UUID id;
    UUID clinicalDayId;
    UUID episodeId;
    UUID scaleId;
    String scaleName;
    String result;
    String rawData;
    LocalDateTime calculatedAt;
    Long calculatedBy;
    LocalDateTime createdAt;
    Integer version;
}
