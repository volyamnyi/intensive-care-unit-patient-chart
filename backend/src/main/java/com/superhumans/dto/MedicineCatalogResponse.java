package com.superhumans.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicineCatalogResponse {
    Long id;
    String name;
    Integer categoryRef;
    String ptgCode;
    Boolean isHighRisk;
}
