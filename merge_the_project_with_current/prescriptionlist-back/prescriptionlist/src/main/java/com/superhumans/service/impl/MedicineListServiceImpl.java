package com.superhumans.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.superhumans.config.SHMedicineListBot;
import com.superhumans.model.medicinelist.*;
import com.superhumans.model.patient.Patient;
import com.superhumans.model.user.User;
import com.superhumans.repository.MedicineListRepository;
import com.superhumans.service.EmailService;
import com.superhumans.service.MedicineService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicineListServiceImpl implements MedicineService {

    MedicineListRepository medicineListRepository;
    SHMedicineListBot notificationBot;
    ObjectMapper objectMapper;
    EmailService emailService;

    @Override
    public List<MedicineList> getAllMedicineLists() {
        return medicineListRepository.getAllMedicineLists();
    }

    @Override
    public MedicineList getMedicineListById(Integer id) {
        return medicineListRepository.getMedicineListById(id);
    }

    @Override
    public List<MedicineList> getAllDocumentsByPatientId(Integer id) {
        return medicineListRepository.getAllDocumentsByPatientId(id);
    }

    @SneakyThrows
    @Override
    public void updateMedicineListById(MedicineList medicineList, Patient patient, String medicineListPage) {

        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        String json = objectWriter.writeValueAsString(medicineList.getMedicineDetails());
        String vlJson = objectWriter.writeValueAsString(medicineList.getVitalList());
        String approvedRowIndexes = objectWriter.writeValueAsString(medicineList.getApprovedRowIndexes());
        medicineListRepository.updateMedicineListById(medicineList, json, vlJson, approvedRowIndexes);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        if (!user.getBusinessRole().equals("NURSE"))
            notificationBot.sendNotification(
                    "✏️ Листок призначень оновлено! " +
                            "\nПацієнт: " + patient.getName() +
                            "\nПалата: " + patient.getRoomNumber() +
                            "\nЛіжко: " + patient.getBedNumber() +
                            "\nЛікуючий лікар: " + patient.getDoctor()
            );

        if (!user.getBusinessRole().equals("NURSE") && !patient.getDoctorUserName().split("\\\\")[1].equals(user.getLogin())) {
            List<String> approvedRowIndexesList = new ArrayList<>(medicineList.getApprovedRowIndexes());
            List<MedicineDetails> approveRequiresMedicine =
                    IntStream.range(0, medicineList.getMedicineDetails().size())
                            .filter(i -> !approvedRowIndexesList.contains(String.valueOf(i)))
                            .mapToObj(i -> medicineList.getMedicineDetails().get(i))
                            .toList();

            StringBuilder approveRequiresMedicineSB = new StringBuilder();
            approveRequiresMedicine.forEach(m -> {
                approveRequiresMedicineSB
                        .append("Назва препарату: ")
                        .append("<strong>")
                        .append(m.getMedicineName())
                        .append("</strong>")
                        .append("<br>");
            });
            approveRequiresMedicineSB.append("Лікар: ")
                    .append("<strong>").append(user.getLastName()).append("</strong>")
                    .append(" ")
                    .append("<strong>")
                    .append(user.getFirstName())
                    .append("</strong>")
                    .append("<br>")
                    .append("Будь ласка, перейдіть за посиланням щоб виконати затвердження:")
                    .append("<br>")
                    .append(medicineListPage)
                    .append("<br>");
            String doctorEmail = patient.getDoctorUserName().split("\\\\")[1] + "@superhumans.com";
            //emailService.sendEmail(doctorEmail, "Листок призначень потребує вашого затвердження", "Наступні призначення потребують вашого затвердження:<br>" + approveRequiresMedicineSB);
        }
    }

    @SneakyThrows
    @Override
    public void createNewMedicineList(MedicineList medicineList, Patient patient) {
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        String json = objectWriter.writeValueAsString(medicineList.getMedicineDetails());
        String vlJson = objectWriter.writeValueAsString(medicineList.getVitalList());
        String approvedRowIndexes = objectWriter.writeValueAsString(medicineList.getApprovedRowIndexes());
        medicineListRepository.createNewMedicineList(medicineList, json, vlJson, approvedRowIndexes);
        notificationBot.sendNotification(
                "✏️ Створено новий листок призначень! " +
                        "\nПацієнт: " + patient.getName() +
                        "\nПалата: " + patient.getRoomNumber() +
                        "\nЛіжко: " + patient.getBedNumber() +
                        "\nЛікуючий лікар: " + patient.getDoctor()
        );
    }

    @SneakyThrows
    //@Scheduled(fixedRate = 300000)
    @Override
    public void autoCreateNewMedicineList() {
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        List<Patient> recentlyPatients = medicineListRepository.getAllRecentlyInpatients();

        final int[] dayCount = {0};

        recentlyPatients.forEach(patient -> {
            if (getAllDocumentsByPatientId(patient.getId()).isEmpty()) {

                dayCount[0] = 0;
                //for (int i = 0; i < 4; i++) {
                MedicineList medicineList = new MedicineList();

                medicineList.setDocumentName("Листок лікарських призначень (стаціонар)");
                medicineList.setMedicineListCreationDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
                medicineList.setMedicineListCreationUser(patient.getDoctorUserName().split("\\\\")[1]);
                medicineList.setPatientRef(patient.getId());

                List<MedicineDetails> detailsList = new ArrayList<>();
                MedicineDetails detail = new MedicineDetails();

                detail.setMedicineListItemId(UUID.randomUUID().toString());
                detail.setMedicineListItemEditDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
                detail.setMedicineListItemEditUser(patient.getDoctorUserName());


                VitalList vitalList = new VitalList();

                //vitalList.setVitalListId(UUID.randomUUID().toString());
                detail.setMedicineListItemEditDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
                detail.setMedicineListItemEditUser(patient.getDoctorUserName());

                List<Day> days = new ArrayList<>();
                List<VitalListDay> vlDays = new ArrayList<>();

                for (int j = 0; j < 21; j++) {

                    Day day = new Day();
                    day.setId(UUID.randomUUID().toString());

                    day.setDate(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(dayCount[0]++).toString().split("T")[0]);

                    DayPart dayPart = new DayPart();

                    dayPart.setId(UUID.randomUUID().toString());
                    day.setMorning(dayPart);

                    dayPart.setId(UUID.randomUUID().toString());
                    day.setDay(dayPart);

                    dayPart.setId(UUID.randomUUID().toString());
                    day.setEvening(dayPart);

                    dayPart.setId(UUID.randomUUID().toString());
                    day.setNight(dayPart);

                    days.add(day);

                    VitalListDay vlDay = new VitalListDay();
                    vlDay.setId(UUID.randomUUID().toString());

                    vlDay.setDate(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(dayCount[0]++).toString().split("T")[0]);

                    VitalListDayPart vlDayPart = new VitalListDayPart();
                    vlDayPart.setId(UUID.randomUUID().toString());
                    vlDay.setMorning(vlDayPart);

                    vlDayPart.setId(UUID.randomUUID().toString());
                    vlDay.setEvening(vlDayPart);

                    vlDays.add(vlDay);
                }

                detail.setMedicineDetails(days);
                vitalList.setVitalList(vlDays);

                detailsList.add(detail);


                medicineList.setMedicineDetails(detailsList);
                medicineList.setVitalList(vitalList);


                String json = "";
                String vlJson = "";
                try {
                    json = objectWriter.writeValueAsString(medicineList.getMedicineDetails());
                    vlJson = objectWriter.writeValueAsString(medicineList.getVitalList());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                medicineListRepository.createNewMedicineList(medicineList, json, vlJson, "[0]");
                notificationBot.sendNotification(
                        "✏️ Створено новий листок призначень! " +
                                "\nПацієнт: " + patient.getName() +
                                "\nПалата: " + patient.getRoomNumber() +
                                "\nЛіжко: " + patient.getBedNumber() +
                                "\nЛікуючий лікар: " + patient.getDoctor());

            }
            // }
        });
    }


    @Override
    public List<Patient> searchPatients(String keyword) {
        return medicineListRepository.searchPatients(keyword);
    }

    @Override
    public Patient getPatientById(Integer id) {
        return medicineListRepository.getPatientById(id);
    }

    @Override
    public List<Medicine> searchMedicine(String keyword) {
        return medicineListRepository.searchMedicine(keyword);
    }

    @Override
    public void deleteMedicineListById(Integer id) {
        medicineListRepository.deleteMedicineListById(id);
    }

    @Override
    public void updateMedicineListStatusByListId(Integer id, String status) {
        medicineListRepository.updateMedicineListStatusByListId(id, status);
    }

    @Override
    public Boolean isDocumentEditing(Integer id) {
        return medicineListRepository.isDocumentEditing(id);
    }

    @Override
    public List<Patient> getAllInpatients(String order, String residence) {
        return medicineListRepository.getAllInpatients(order, residence);
    }

    @Override
    public void generateDeDocument(Integer medicineListID, String documentDateTime) {
        medicineListRepository.generateDeDocument(medicineListID, documentDateTime);
    }

    @Override
    public List<Allergies> getAllAllergiesByPatientId(Integer id) {
        return medicineListRepository.getAllAllergiesByPatientId(id);
    }

    @Override
    public List<Medicine> getHighRiskMedicineByName(String highRiskMedicineName) {
        return medicineListRepository.getHighRiskMedicineByName(highRiskMedicineName);
    }

    @Override
    public List<Medicine> getConflictMedicineByName(String conflictMedicineName) {
        return medicineListRepository.getConflictMedicineByName(conflictMedicineName);
    }

    @Override
    public void closeMedicineListByListId(Integer id) {
        medicineListRepository.closeMedicineListByListId(id);
    }

}
