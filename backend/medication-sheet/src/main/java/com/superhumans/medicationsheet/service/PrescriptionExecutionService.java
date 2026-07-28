package com.superhumans.medicationsheet.service;

import com.superhumans.entity.User;
import com.superhumans.entity.UserRole;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionExecutionRepository;
import com.superhumans.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PrescriptionExecution execute(UUID dayPartId, Long currentUserId, String currentUserLogin, String actualDose, String secondPersonLogin, String secondPersonPassword) {
        PrescriptionDayPart part = partRepository.findById(dayPartId)
                .orElseThrow(() -> new NotFoundException("Day part not found: " + dayPartId));

        // Authenticate second person
        User secondPerson = userRepository.findByLogin(secondPersonLogin)
                .orElseThrow(() -> new IllegalArgumentException("Другу особу не знайдено: " + secondPersonLogin));

        if (!passwordEncoder.matches(secondPersonPassword, secondPerson.getPasswordHash())) {
            throw new IllegalArgumentException("Невірний пароль для другої особи");
        }

        if (secondPerson.getRole() != UserRole.NURSE) {
            throw new IllegalArgumentException("Друга особа повинна мати роль медсестри/медичного брата");
        }

        if (secondPerson.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Друга особа не може бути тією ж, що виконує призначення");
        }

        // Deterministic UUIDs from logins for audit trail
        UUID firstPersonUuid = UUID.nameUUIDFromBytes(currentUserLogin.getBytes());
        UUID secondPersonUuid = UUID.nameUUIDFromBytes(secondPersonLogin.getBytes());

        PrescriptionExecution exec = PrescriptionExecution.builder()
                .dayPart(part)
                .executedAt(LocalDateTime.now())
                .actualDose(actualDose)
                .status("Completed")
                .requires2pAuth(true)
                .secondPersonId(secondPersonUuid)
                .build();
        exec.setCreatedBy(currentUserId);
        exec.setUpdatedBy(currentUserId);
        exec.setExecutedBy(firstPersonUuid);
        exec = executionRepository.save(exec);

        part.setIsCompleted(true);
        part.setNurseName(currentUserLogin + "/2P:" + secondPersonLogin);
        part.setUpdatedBy(currentUserId);
        partRepository.save(part);

        log.info("Dose executed: dayPartId={}, firstPersonLogin={}, secondPersonLogin={}",
                dayPartId, currentUserLogin, secondPersonLogin);
        return exec;
    }
}
