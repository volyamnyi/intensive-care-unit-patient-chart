package com.superhumans.mis;

import com.superhumans.mis.dto.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MIS integration interface.
 * <p>
 * <b>POLICY: ICU Chart is a READ-ONLY client of MIS.</b>
 * Only data retrieval (read) from MIS is permitted.
 * The sole exception is {@link #sendPdf(UUID, byte[], String, int)} which transfers
 * an immutable PDF document — no existing MIS records are modified.
 * <p>
 * <b>FORBIDDEN:</b> Any MIS write method (spzIBPatientCreate, spzIBScheduleCreate,
 * spzIBAgentSave, spzIBInstitutionSave, etc.) MUST NEVER be called by this application.
 * Violating this policy will corrupt MIS data integrity.
 */
public interface MisService {

    Optional<PatientDTO> getPatient(Long patientId);

    Optional<HospitalizationDTO> getHospitalization(UUID hospitalizationId);

    Optional<UserMisDTO> getUser(Long userId);

    List<UserMisDTO> getDepartmentUsers(Long departmentId);

    List<DepartmentDTO> getDepartments();

    List<DictionaryItemDTO> getDictionary(String dictionaryName);

    List<PatientDTO> searchPatients(String query);

    /**
     * Sends generated PDF to MIS patient document repository.
     * This is the ONLY allowed write operation to MIS.
     * The PDF is immutable — no existing MIS records are modified.
     */
    boolean sendPdf(UUID clinicalDayId, byte[] pdfContent, String fileName, int version);

    /**
     * Searches medicine catalog from MIS.
     * Results are cached locally in medicine_catalog_cache table.
     */
    List<MedicineMisDTO> searchMedicineCatalog(String keyword);

    /**
     * Retrieves patient allergies from MIS.
     * Results are cached locally in allergy_cache table.
     */
    List<AllergyMisDTO> getPatientAllergies(Long patientId);

    /**
     * Retrieves the list of services (послуги) from MIS — spzIBServiceList.
     * Services carry ISO 9999 / product codes used by prosthetics order templates.
     */
    List<ServiceMisDTO> getServices();

    /**
     * Retrieves patient bookings (бронювання послуг) from MIS — spzIBBookingList.
     * Bookings carry service codes, names and dates used by prosthetics order templates.
     */
    List<BookingMisDTO> getPatientBookings(Long patientId);

    /**
     * Retrieves patient documents from MIS — spzIBDocumentList.
     * Used to link order templates with MIS patient documents.
     */
    List<DocumentMisDTO> getPatientDocuments(Long patientId);

    /**
     * Retrieves extended patient info (account, bookings, debt) from MIS — spzIBPatientInfo.
     * Used by prosthetics order templates for the "Загальні відомості про особу" section.
     */
    Optional<PatientInfoMisDTO> getPatientInfo(Long patientId);

    /**
     * Sets the MIS error-simulation mode (testing hook) on the active MIS implementation.
     * Modes: none, timeout, not_found, unavailable. Implementations that do not simulate
     * errors keep this a no-op.
     */
    default void setErrorMode(String mode) {
        // no-op by default
    }
}
