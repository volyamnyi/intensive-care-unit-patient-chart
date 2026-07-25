package com.superhumans.model.medicinelist;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicineDetails {

    @JsonProperty("id")
    String medicineListItemId;

    String medicineListItemEditUser;

    String medicineMethod = "";


    String regime = "";

    String medicineName = "";

    LocalDateTime medicineListItemEditDate;

    List<Day> medicineDetails;

    String status = "";

    @Override
    public String toString() {
        return "MedicineDetails{" +
                "medicineListItemId='" + medicineListItemId + '\'' +
                ", medicineListItemEditUser='" + medicineListItemEditUser + '\'' +
                ", medicineMethod='" + medicineMethod + '\'' +
                ", regime='" + regime + '\'' +
                ", medicineName='" + medicineName + '\'' +
                ", medicineListItemEditDate=" + medicineListItemEditDate +
                ", medicineDetails=" + medicineDetails +
                ", status='" + status + '\'' +
                '}';
    }
}
