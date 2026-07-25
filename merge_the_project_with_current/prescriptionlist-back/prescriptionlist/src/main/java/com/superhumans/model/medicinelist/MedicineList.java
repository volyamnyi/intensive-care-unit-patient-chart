package com.superhumans.model.medicinelist;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeSet;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicineList {

    Integer medicineListID;

	Integer patientRef;

    String documentName;

	String medicineListCreationUser;

    LocalDateTime medicineListCreationDate;

    String status;

    @JsonProperty("medicineDetails")
    List<MedicineDetails> medicineDetails;

    @JsonProperty("vitalList")
    VitalList vitalList;

    TreeSet<String> approvedRowIndexes;

    @Override
    public String toString() {
        return "MedicineList{" +
                "medicineListID=" + medicineListID +
                ", patientRef=" + patientRef +
                ", documentName='" + documentName + '\'' +
                ", medicineListCreationUser='" + medicineListCreationUser + '\'' +
                ", medicineListCreationDate=" + medicineListCreationDate +
                ", medicineDetails=" + medicineDetails +
                ", approvedRowIndexes=" + approvedRowIndexes +
                '}';
    }
}
