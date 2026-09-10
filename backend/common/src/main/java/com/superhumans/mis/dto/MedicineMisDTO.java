package com.superhumans.mis.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

/**
 * MIS medicine item (real mode: {@code spiMedicineItemKindDetails}).
 * Fields beyond the legacy dictionary envelope (itemKind*,
 * medicineCategory*, medicinePackage*) are assumptions — the
 * epic MIS real-API spec is still open (#258 follow-up). The parser is
 * tolerant: missing fields resolve to {@code null}, not errors.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicineMisDTO {
    Long id;
    String name;
    Integer categoryRef;
    String ptgCode;
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
