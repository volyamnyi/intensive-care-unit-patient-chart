package com.superhumans.medicationsheet.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicineCatalogResponse {
    Long id;
    String name;
    Integer categoryRef;
    String ptgCode;
    Boolean isHighRisk;
    String itemKindCode;
    String itemKindAtc;
    String itemKindUnit;
    String itemKindManufacturer;
    Boolean itemKindIsDisabled;
    String itemKindEan;
    Boolean itemKindIsDivisible;
    String itemKindDlc;
    Long medicineCategoryId;
    String medicineCategoryName;
    Long medicinePackageId;
    String medicinePackageName;
}
