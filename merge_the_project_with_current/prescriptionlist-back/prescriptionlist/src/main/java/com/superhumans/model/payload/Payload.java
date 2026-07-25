package com.superhumans.model.payload;

import com.superhumans.model.medicinelist.MedicineList;
import com.superhumans.model.patient.Patient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payload {
    MedicineList medicineList;
    Patient patient;
    String medicineListPage;
}
