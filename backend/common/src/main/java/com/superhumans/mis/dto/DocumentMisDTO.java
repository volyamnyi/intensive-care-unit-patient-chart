package com.superhumans.mis.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * MIS document (документ пацієнта: prosthetics order / conclusion).
 * Used to link order templates with MIS patient documents and to decide
 * prosthetics eligibility (template 120/121).
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
    /** Direct link to the MIS document (populated by spiDocumentProsthesCheck). */
    String documentUrl;
    Long patientId;
    LocalDateTime orderDate;
    String patientFullName;
    String patientAddress;
    String productCode;
    String productName;
    String mobilityLevel;
    String patientGender;
    Integer age;
    Integer height;
    Integer weight;
    String note;
}
