package com.superhumans.medicationsheet.service;

import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionExecutionService {

    private final PrescriptionExecutionRepository executionRepository;
    private final PrescriptionDayPartRepository partRepository;
    private final DrugInteractionService drugInteractionService;

    @Transactional
    public PrescriptionExecution execute(UUID dayPartId, UUID nurseId, String actualDose, boolean requires2p, UUID secondPersonId) {
        PrescriptionDayPart part = partRepository.findById(dayPartId)
                .orElseThrow(() -> new NotFoundException("Day part not found: " + dayPartId));

        PrescriptionExecution exec = PrescriptionExecution.builder()
                .dayPart(part)
                .executedAt(LocalDateTime.now())
                .actualDose(actualDose)
                .status("Completed")
                .requires2pAuth(requires2p)
                .secondPersonId(secondPersonId)
                .build();
        exec.setCreatedBy(0L);
        exec.setUpdatedBy(0L);
        exec.setExecutedBy(nurseId);
        exec = executionRepository.save(exec);

        part.setIsCompleted(true);
        part.setNurseName(nurseId.toString());
        if (secondPersonId != null) {
            part.setNurseName(nurseId + "/2P:" + secondPersonId);
        }
        part.setUpdatedBy(0L);
        partRepository.save(part);

        log.info("Dose executed: dayPartId={}, nurseId={}, requires2p={}", dayPartId, nurseId, requires2p);
        return exec;
    }

    @Transactional(readOnly = true)
    public boolean requires2pAuth(int medicineCategoryRef) {
        return drugInteractionService.isHighRisk(medicineCategoryRef);
    }
}
