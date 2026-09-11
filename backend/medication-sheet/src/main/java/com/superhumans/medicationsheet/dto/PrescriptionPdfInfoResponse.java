package com.superhumans.medicationsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Batch metadata for the Form №003-4/о PDF download (no PII). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionPdfInfoResponse {
    int pages;
    String fileName;
}
