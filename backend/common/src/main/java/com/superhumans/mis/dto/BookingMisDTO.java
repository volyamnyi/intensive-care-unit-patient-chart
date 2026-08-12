package com.superhumans.mis.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * MIS booking (бронювання послуги пацієнта) from spzIBBookingList.
 * Used to populate materials/fittings data in prosthetics order templates.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingMisDTO {
    Long bookingId;
    String bookingName;
    LocalDateTime bookingDate;
    Long patientId;
    Long serviceId;
    String serviceCode;
    Double bookingServicePriceValue;
    Integer bookingQuantity;
    String bookingStatusCode;
    String bookingPaymentStatusCode;
    LocalDateTime bookingCreationDate;
    String bookingExecutionUserLogin;
}
