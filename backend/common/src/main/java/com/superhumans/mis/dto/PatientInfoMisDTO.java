package com.superhumans.mis.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

/**
 * Extended MIS patient info from spzIBPatientInfo — patient account, bookings to pay, debt.
 * Used by prosthetics order templates for the "Загальні відомості про особу" section.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PatientInfoMisDTO {
    Long patientId;
    String patientName;
    LocalDate patientBirthDate;
    String patientAddress;
    String patientPhone;
    String patientEmail;
    String patientSexCode;
    Double accountValue;
    String companyEDRPOU;
    String companyName;
    Double patientBookingSum;
    List<BookingMisDTO> patientBookingAct;
    Double patientDebtSum;
}
