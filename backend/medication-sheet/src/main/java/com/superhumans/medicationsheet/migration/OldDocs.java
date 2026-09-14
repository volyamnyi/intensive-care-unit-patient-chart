package com.superhumans.medicationsheet.migration;

/** Parsed CSV rows of the legacy export (column positions are fixed). */
public final class OldDocs {

    private OldDocs() {
    }

    /** One {@code MedicineList.csv} data row. */
    public record OldListDoc(String oldId, String patientRef, String documentName,
            String creationUser, String creationDate) {

        public boolean prescription() {
            return ImportConverters.isPrescriptionDocument(documentName);
        }
    }

    /** One {@code MedicineListItem.csv} data row. */
    public record OldItemDoc(String oldId, String listRef, String editUser, String editDate,
            String medicineDetails, String vitalList) {
    }
}
