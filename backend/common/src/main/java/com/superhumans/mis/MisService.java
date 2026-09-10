package com.superhumans.mis;

import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import java.util.List;
import java.util.Optional;

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

    Optional<PatientDTO> getPatient(Long patientId);

    List<PatientDTO> searchPatients(String query);

    /**
     * Returns every patient currently under treatment — the single base source
     * of patient data for all modules (real MIS: {@code spiPatientProsthesCheck}).
     * Module use-cases apply their own department rules on top of this list and
     * must not implement alternative patient sources.
     */
    List<PatientDTO> getAllPatientsUnderTreatment();

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
