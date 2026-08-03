package com.superhumans.medicationsheet.service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import com.superhumans.mis.MisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduling.auto-create-prescriptions-enabled", havingValue = "true")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrescriptionSchedulerService {

    MisService misService;
    PrescriptionListRepository listRepository;
    PrescriptionListService listService;
    NotificationService notificationService;

    @Scheduled(fixedRate = 300000)
    public void autoCreatePrescriptionLists() {
        log.debug("Auto-create prescription list check started");

        var allPatients = misService.searchPatients("");
        int created = 0;

        for (var patient : allPatients) {
            List<PrescriptionList> existing = listRepository.findByPatientId(patient.getId());
            boolean hasAny = existing.stream().anyMatch(l -> !l.getDeleted());
            if (hasAny) {
                continue;
            }
            PrescriptionList list = listService.create(patient.getId());
            notificationService.notifyPrescriptionCreated(list.getId(), patient.getFullName());
            created++;
        }

        if (created > 0) {
            log.info("Auto-created {} prescription list(s) for new patients", created);
        } else {
            log.debug("No new prescription lists needed");
        }
    }
}
