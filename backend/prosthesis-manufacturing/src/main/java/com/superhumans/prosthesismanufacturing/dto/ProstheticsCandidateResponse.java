package com.superhumans.prosthesismanufacturing.dto;

import com.superhumans.mis.dto.DocumentMisDTO;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Single prosthetics candidate for the prosthetist worklist (Phase 6, #259).
 * The backend hands the frontend a ready-to-render candidate: the patient
 * (MIS demographics merged with local clinical data), the patient's local
 * orders, and the matching MIS documents (templates 120/121) carrying
 * {@code documentUrl}. No further MIS calls are needed to render the list —
 * the frontend consumes this contract in Phase 9.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProstheticsCandidateResponse {
    ProstheticsPatientResponse patient;
    List<ProstheticsOrderResponse> orders;
    List<DocumentMisDTO> documents;
    /**
     * True when the per-patient MIS document fetch failed. The patient stays
     * in the list (fail-open with an explicit mark) so one MIS error never
     * hides the whole worklist; {@code documents} is empty in that case.
     */
    boolean documentsUnknown;
}
