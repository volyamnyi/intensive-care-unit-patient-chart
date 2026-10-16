package com.superhumans.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Scale-catalog entry (#411 item 6): the {@code GET /api/scales} endpoint
 * used to return raw {@code ClinicalScale} JPA entities (audit fields, version
 * and every future column become API surface). Only catalog fields travel.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClinicalScaleResponse {
    UUID id;
    String name;
    String description;
    Boolean isAutomatic;
    String status;
}
