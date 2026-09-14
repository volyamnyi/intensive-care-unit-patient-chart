package com.superhumans.mis;

import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * MIS integration interface.
 * <p>
 * <b>POLICY: ICU Chart is a READ-ONLY client of MIS — with no exceptions.</b>
 * Only data retrieval (read) from MIS is permitted. Generated PDFs stay local
 * (download/print in-module); they are never transferred to MIS.
 * <p>
 * <b>FORBIDDEN:</b> Any MIS write method ({@code Save/Create/Update/Delete}
 * procedure families, document transfer, etc.) MUST NEVER be called by this
 * application. Violating this policy will corrupt MIS data integrity.
 */
public interface MisService {

    /** MIS stay states that exclude a patient from treatment views. */
    Set<String> NON_TREATMENT_STATUSES = Set.of("MOV", "CMP", "CNC", "REJ");

    /**
     * Treatment check: a patient counts as under treatment unless MIS reports
     * a known terminal status. Null, blank and unknown codes fail open — a
     * patient is never hidden from care delivery because of an unrecognized
     * code (unknown codes surface verbatim in the UI instead).
     */
    static boolean isUnderTreatment(PatientDTO patient) {
        if (patient == null || patient.getPatientStatus() == null
                || patient.getPatientStatus().isBlank()) {
            return true;
        }
        return !NON_TREATMENT_STATUSES.contains(patient.getPatientStatus().strip());
    }

    Optional<PatientDTO> getPatient(Long patientId);

    List<PatientDTO> searchPatients(String query);

    /**
     * Paged pool of patients for the «all patients» views, with optional
     * query/status filters. One bulk MIS fetch per call, sliced in memory —
     * callers must never fan out per-patient requests over the pool.
     */
    Page<PatientDTO> getPatientPool(String query, String status, Pageable pageable);

    /**
     * Returns every patient the procedure currently reports, regardless of
     * stay status — the raw pool (real MIS: {@code spiPatientProsthesCheck}).
     * Callers that need only patients under treatment must use
     * {@link #getPatientsUnderTreatment()} instead.
     */
    List<PatientDTO> getAllPatients();

    /**
     * Patients under treatment: the raw pool minus {@link #NON_TREATMENT_STATUSES}.
     * The single base source of patient data for treatment views in all modules.
     */
    default List<PatientDTO> getPatientsUnderTreatment() {
        return getAllPatients().stream().filter(MisService::isUnderTreatment).toList();
    }

    /**
     * Searches the medicine catalog from MIS (real mode:
     * {@code spiMedicineItemKindDetails}). Read live on every call — no local
     * cache. Pre-#258 the result was denormalised into
     * {@code medicine_catalog_cache}; that table is dropped in
     * {@code med/002-drop-allergy-and-medicine-cache.sql} and the legacy
     * dictionary call site is replaced by the SPI call.
     */
    List<MedicineMisDTO> searchMedicineCatalog(String keyword);

    /**
     * Retrieves patient documents from MIS (real mode: {@code spiDocumentProsthesCheck}).
     * Used to link order templates with MIS patient documents.
     */
    List<DocumentMisDTO> getPatientDocuments(Long patientId);
}
