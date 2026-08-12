package com.superhumans.mis.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * MIS document (документ пацієнта) from spzIBDocumentList / spzIBPatientDocumentList.
 * Used to link order templates with MIS patient documents.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentMisDTO {
    Long documentId;
    String documentName;
    LocalDateTime documentCreationDate;
    String documentUserLogin;
    Long documentTemplateId;
    String documentTemplateName;
    String documentKindCode;
    String documentKindName;
    String documentApproveStatusCode;
    String documentApproveStatusName;
    String documentExternalId;
}
