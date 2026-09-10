package com.superhumans.mis.dto;

import lombok.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * MIS patient ({@code spiPatientProsthesCheck}) — exact 13-field contract:
 * {@code id, fullName, birthDate, sexCode, address, phone, email, bloodGroup,
 * rhFactor, departmentId, room, bed, doctorName}.
 * <p>
 * {@code birthDate} arrives as {@code "1962-07-08T00:00:00"} and is stored as
 * {@link LocalDateTime} so the time component round-trips through
 * {@code /api/patients}. MIS no longer supplies external card numbers,
 * height or weight — those fields were removed (no fallbacks, no aliases).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PatientDTO {
    Long id;
    String fullName;
    LocalDateTime birthDate;
    String sexCode;
    String address;
    String phone;
    String email;
    String bloodGroup;
    String rhFactor;
    Long departmentId;
    String room;
    String bed;
    String doctorName;
}
