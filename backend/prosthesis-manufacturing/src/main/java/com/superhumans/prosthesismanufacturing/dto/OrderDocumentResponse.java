package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Read-only link to the order document hosted in MIS. Resolved by
 * {@code spiDocumentProsthesCheck} (real mode) or the legacy
 * {@code spzIBDocumentList} envelope. The application never generates
 * or proxies PDF bytes — the client opens {@link #documentUrl} directly.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDocumentResponse {
    Long documentId;
    String documentTemplateName;
    String documentUrl;
}
