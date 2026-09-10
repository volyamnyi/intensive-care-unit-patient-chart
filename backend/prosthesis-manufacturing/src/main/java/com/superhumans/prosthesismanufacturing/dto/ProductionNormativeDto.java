package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Editable production normative thresholds (manufacturing epic #271).
 * Stored in {@code SystemSettings}; absent or corrupt rows fall back to
 * {@link com.superhumans.prosthesismanufacturing.service.ProductionNormativeService#DEFAULT_OVERDUE_MULTIPLIER}
 * and {@code DEFAULT_STALE_DAYS}.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductionNormativeDto {
    Double overdueMultiplier;
    Integer staleDays;
}
