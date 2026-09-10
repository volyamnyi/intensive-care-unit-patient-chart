package com.superhumans.prosthesismanufacturing.dto;

import com.superhumans.mis.dto.DocumentMisDTO;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

/**
 * Detail view for one production work item (manufacturing epic #271).
 * Aggregates the dashboard row, the execution timeline, quality data and the
 * linked order/patient — patient personal data and MIS documents are present
 * only when the caller holds {@code PROSTHETICS_PRODUCTION_PATIENT_VIEW}
 * ({@code patientDetailsVisible}); otherwise {@code patient} carries the PIB
 * only and {@code documents} is empty.
 */
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductionDetailDto {
    ProductionWorkItemDto workItem;
    List<StepExecutionResponse> timeline;
    List<BrakEventResponse> brakEvents;
    List<FlowInstanceResponse> branches;
    ProstheticsOrderResponse order;
    ProstheticsPatientResponse patient;
    boolean patientDetailsVisible;
    List<DocumentMisDTO> documents;
    /** Document matched to this order ({@code MIS-{patient}-{document}} first, else first with URL). */
    DocumentMisDTO matchedDocument;
    /** True when the MIS patient link is corrupt and no documents could load. */
    boolean documentsUnknown;
}
