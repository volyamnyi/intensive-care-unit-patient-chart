package com.superhumans.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.superhumans.model.medicinelist.Allergies;
import com.superhumans.model.medicinelist.Medicine;
import com.superhumans.model.medicinelist.MedicineList;
import com.superhumans.model.patient.Patient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface MedicineService {
    void createNewMedicineList(MedicineList medicineList, Patient patient) throws JsonProcessingException;

    List<MedicineList> getAllMedicineLists();
    List<MedicineList> getAllDocumentsByPatientId(Integer id);

    MedicineList getMedicineListById(Integer id);

    void updateMedicineListById(MedicineList medicineList, Patient patient, String medicineListPage);

    void autoCreateNewMedicineList();

    List<Patient> searchPatients(String keyword);

    Patient getPatientById(Integer id);


    List<Medicine> searchMedicine(String keyword);

    void deleteMedicineListById(Integer id);

    void updateMedicineListStatusByListId(Integer id, String status);

    Boolean isDocumentEditing(Integer id);

    List<Patient> getAllInpatients(String order, String residence);

    void generateDeDocument(Integer patientId, String documentDateTime);

    List<Allergies> getAllAllergiesByPatientId(Integer id);

    List<Medicine> getHighRiskMedicineByName(String highRiskMedicineName);

    List<Medicine> getConflictMedicineByName(String conflictMedicineName);

    void closeMedicineListByListId(Integer id);
}
