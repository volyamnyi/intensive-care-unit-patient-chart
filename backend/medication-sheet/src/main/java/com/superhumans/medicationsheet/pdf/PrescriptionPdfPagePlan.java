package com.superhumans.medicationsheet.pdf;

import com.superhumans.medicationsheet.pdf.PrescriptionPdfPrintModel.Cell;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Immutable physical sheet. Y coordinates grow downwards from the top of the page. */
public record PrescriptionPdfPagePlan(int pageIndex, LocalDate week, Header header, Geometry geometry,
                                      List<Placed> rows, List<String> signatures, List<String> footerNotes) {
    /** There is no verified chart number, attending-doctor login or patient-level regime in the source contract. */
    public record Header(String institution, String edrpou, String authority, String address, boolean logo,
                         String patient, String room) { }
    record Geometry(float top, float authorityHeight, float institutionHeight, float patientHeight,
                    float bodyTop, float footerTop) { }
    record Block(UUID itemId, int number, List<String> label, List<String> details, Map<Integer, Cell> owners) { }
    record Placed(Block block, float top, float pairHeight, float height) { }
}
