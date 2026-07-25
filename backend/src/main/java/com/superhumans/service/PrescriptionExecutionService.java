package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.exception.NotFoundException;
import com.superhumans.repository.*;
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
    public PrescriptionExecution execute(UUID dayPartId, Long nurseId, String actualDose, boolean requires2p, Long secondPersonId) {
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
        exec.setCreatedBy(nurseId);
        exec.setUpdatedBy(nurseId);
        exec.setExecutedBy(nurseId);
        exec = executionRepository.save(exec);

        part.setIsCompleted(true);
        part.setNurseName(nurseId.toString());
        if (secondPersonId != null) {
            part.setNurseName(nurseId + "/2P:" + secondPersonId);
        }
        part.setUpdatedBy(nurseId);
        partRepository.save(part);

        log.info("Dose executed: dayPartId={}, nurseId={}, requires2p={}", dayPartId, nurseId, requires2p);
        return exec;
    }

    @Transactional(readOnly = true)
    public boolean requires2pAuth(int medicineCategoryRef) {
        return drugInteractionService.isHighRisk(medicineCategoryRef);
    }
}
