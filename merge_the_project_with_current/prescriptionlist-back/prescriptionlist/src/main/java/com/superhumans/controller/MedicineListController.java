package com.superhumans.controller;

import com.superhumans.config.SHMedicineListBot;
import com.superhumans.exception.AppException;
import com.superhumans.model.medicinelist.Allergies;
import com.superhumans.model.medicinelist.Medicine;
import com.superhumans.model.medicinelist.MedicineList;
import com.superhumans.model.patient.Patient;
import com.superhumans.model.payload.Payload;
import com.superhumans.service.MedicineService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/medicinelist")
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.24.32:5173"}, allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.GET})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicineListController {

    final MedicineService medicineService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<MedicineList> getAllMedicineLists() {
        return medicineService.getAllMedicineLists();
    }

    @GetMapping("/bylist/{id}")
    @ResponseStatus(HttpStatus.OK)
    public MedicineList getMedicineListById(@PathVariable("id") Integer id) {
        if (id != null)
            return medicineService.getMedicineListById(id);
        throw new AppException("Документу з " + id + " не знайдено", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/bypatient/{id}")
    @ResponseStatus(HttpStatus.OK)
    public List<MedicineList> getAllDocumentsByPatientId(@PathVariable("id") Integer id) {
        if (id != null)
            return medicineService.getAllDocumentsByPatientId(id);
        throw new AppException("Пацієнт з " + id + " немає документів", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/allergies/bypatient/{id}")
    @ResponseStatus(HttpStatus.OK)
    public List<Allergies> getAllAllergiesByPatientId(@PathVariable("id") Integer id) {

        if (id != null)
            return medicineService.getAllAllergiesByPatientId(id);
        throw new AppException("Пацієнт з " + id + " немає алергій", HttpStatus.NOT_FOUND);
    }

    @SneakyThrows
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createNewMedicineList(@RequestBody Payload payload) {
        MedicineList medicineList = payload.getMedicineList();
        Patient patient = payload.getPatient();
        medicineService.createNewMedicineList(medicineList, patient);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public void updateMedicineList(@RequestBody Payload payload) {
        MedicineList medicineList = payload.getMedicineList();
        System.out.println(medicineList.getVitalList());
        Patient patient = payload.getPatient();
        String medicineListPage = payload.getMedicineListPage();
        medicineService.updateMedicineListById(medicineList, patient, medicineListPage);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateMedicineListStatusByListId(@PathVariable Integer id, @RequestParam String status) {
        medicineService.updateMedicineListStatusByListId(id, status);
    }

    @PutMapping("/closelist/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void closeMedicineListByListId(@PathVariable("id") Integer id) {
        medicineService.closeMedicineListByListId(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteMedicineListById(@PathVariable Integer id) {
        if (id != null)
            medicineService.deleteMedicineListById(id);
        throw new AppException("Документу з " + id + " не знайдено", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/searchpatients")
    @ResponseStatus(HttpStatus.OK)
    public List<Patient> searchPatients(@RequestParam String keyword) {
        return medicineService.searchPatients(keyword);
    }

    @GetMapping("/searchmedicine")
    @ResponseStatus(HttpStatus.OK)
    public List<Medicine> searchMedicine(@RequestParam String keyword) {
        return medicineService.searchMedicine(keyword);
    }

    @GetMapping("/medicine/getHighRiskMedicineByName")
    @ResponseStatus(HttpStatus.OK)
    public List<Medicine> getHighRiskMedicineByName(@RequestParam String highRiskMedicineName) {
        return medicineService.getHighRiskMedicineByName(highRiskMedicineName);
    }

    @GetMapping("/medicine/getConflictMedicineByName")
    @ResponseStatus(HttpStatus.OK)
    public List<Medicine> getConflictMedicineByName(@RequestParam String conflictMedicineName) {
        return medicineService.getConflictMedicineByName(conflictMedicineName);
    }

    @GetMapping("/patient/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Patient getPatientById(@PathVariable("id") Integer id) {
        if (id != null)
            return medicineService.getPatientById(id);
        throw new AppException("Пацієнта з " + id + " не знайдено", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/patient/sort")
    @ResponseStatus(HttpStatus.OK)
    public List<Patient> getAllInpatients(@RequestParam("order") String order, @RequestParam("residence") String residence) {
        return medicineService.getAllInpatients(order, residence);
    }

    @GetMapping("/isDocumentEditing/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Boolean isDocumentEditing(@PathVariable("id") Integer id) {
        if (id != null)
            return medicineService.isDocumentEditing(id);
        throw new AppException("Документу з " + id + " не знайдено", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/generatedoc")
    @ResponseStatus(HttpStatus.OK)
    public void generateDeDocument(@RequestParam Integer medicineListID, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String documentDateTime) {
        medicineService.generateDeDocument(medicineListID, documentDateTime);
    }

}
