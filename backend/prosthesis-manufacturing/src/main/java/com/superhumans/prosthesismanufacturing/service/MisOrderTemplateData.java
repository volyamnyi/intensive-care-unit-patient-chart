package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.mis.dto.BookingMisDTO;
import com.superhumans.mis.dto.DepartmentDTO;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.mis.dto.PatientInfoMisDTO;
import com.superhumans.mis.dto.ServiceMisDTO;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Aggregated MIS data used to render prosthetics order templates.
 * Every field is nullable — the template falls back to locally stored
 * patient/order data when MIS data is unavailable.
 */
@Value
@Builder
public class MisOrderTemplateData {

    /** Extended patient info from spzIBPatientInfo (personal data section). */
    PatientInfoMisDTO patientInfo;

    /** Company/department (header block: name, address, EDRPOU). */
    DepartmentDTO company;

    /** Doctor name resolved from spzIBUserDetails. */
    String doctorName;

    /** Technician name resolved from spzIBUserDetails. */
    String technicianName;

    /** Services (послуги) with ISO 9999 product codes from spzIBServiceList. */
    List<ServiceMisDTO> services;

    /** Patient bookings (матеріали/комплектуючі) from spzIBBookingList. */
    List<BookingMisDTO> bookings;

    /** Patient documents from spzIBDocumentList. */
    List<DocumentMisDTO> documents;
}
