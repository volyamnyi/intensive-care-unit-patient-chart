package com.superhumans.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import com.superhumans.util.ClinicalConstants;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HourlyRecordCreateRequest {
    @NotNull
    LocalDateTime recordTime;
    String consciousness;

    @Min(ClinicalConstants.GCS_MIN) @Max(ClinicalConstants.GCS_MAX)
    Integer gcs;

    @DecimalMin(ClinicalConstants.TEMPERATURE_MIN_STR) @DecimalMax(ClinicalConstants.TEMPERATURE_MAX_STR)
    Double temperature;
    @Min(0) @Max(300)
    Integer heartRate;
    @Min(0) @Max(60)
    Integer respiratoryRate;
    @Min(50) @Max(250)
    Integer systolicBP;
    @Min(30) @Max(150)
    Integer diastolicBP;
    Integer meanArterialPressure;
    @DecimalMin("50.0") @DecimalMax("100.0")
    Double spo2;
    @DecimalMin("0.0") @DecimalMax("100.0")
    Double etco2;
    @DecimalMin(ClinicalConstants.FIO2_MIN_STR) @DecimalMax(ClinicalConstants.FIO2_MAX_STR)
    Double fio2;
    @DecimalMin(ClinicalConstants.CVP_MIN_STR) @DecimalMax(ClinicalConstants.CVP_MAX_STR)
    Double cvp;
    @DecimalMin(ClinicalConstants.VASOPRESSOR_MIN_STR) @DecimalMax(ClinicalConstants.VASOPRESSOR_MAX_STR)
    Double dopamine;
    @DecimalMin(ClinicalConstants.VASOPRESSOR_MIN_STR) @DecimalMax(ClinicalConstants.VASOPRESSOR_MAX_STR)
    Double dobutamine;
    @DecimalMin(ClinicalConstants.VASOPRESSOR_MIN_STR) @DecimalMax(ClinicalConstants.VASOPRESSOR_MAX_STR)
    Double norepinephrine;
    @DecimalMin(ClinicalConstants.VASOPRESSOR_MIN_STR) @DecimalMax(ClinicalConstants.VASOPRESSOR_MAX_STR)
    Double epinephrine;
    @DecimalMin("0.0")
    Double urineOutput;
    @DecimalMin("0.0")
    Double drainOutput;
    String stool;
    String vomit;
    @Min(0) @Max(10)
    Integer painScore;
    String notes;
}
