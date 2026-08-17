package com.superhumans.medicationsheet.service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionExecutionRepository;
import com.superhumans.repository.core.UserRepository;
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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrescriptionExecutionService {

    PrescriptionExecutionRepository executionRepository;
    PrescriptionDayPartRepository partRepository;
    DrugInteractionService drugInteractionService;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    @Transactional
    public PrescriptionExecution execute(UUID dayPartId, Long currentUserId, String currentUserLogin, String actualDose, String secondPersonLogin, String secondPersonPassword, boolean requires2pAuth) {
        PrescriptionDayPart part = partRepository.findById(dayPartId)
                .orElseThrow(() -> new NotFoundException("Day part not found: " + dayPartId));

        UUID firstPersonUuid = UUID.nameUUIDFromBytes(currentUserLogin.getBytes());
        UUID secondPersonUuid = null;
        String nurseName = currentUserLogin;

        if (requires2pAuth) {
            // Authenticate second person
            User secondPerson = userRepository.findByLogin(secondPersonLogin)
                    .orElseThrow(() -> new IllegalArgumentException("Помилка автентифікації другої особи"));

            if (!passwordEncoder.matches(secondPersonPassword, secondPerson.getPasswordHash())) {
                throw new IllegalArgumentException("Невірний пароль другої особи");
            }

            if (secondPerson.getRole() != UserRole.NURSE) {
                throw new IllegalArgumentException("Друга особа повинна мати роль медсестри");
            }

            if (secondPerson.getId().equals(currentUserId)) {
                throw new IllegalArgumentException("Друга особа не може бути тією ж, що виконує призначення");
            }

            secondPersonUuid = UUID.nameUUIDFromBytes(secondPersonLogin.getBytes());
            nurseName = currentUserLogin + "/2P:" + secondPersonLogin;
        }

        PrescriptionExecution exec = PrescriptionExecution.builder()
                .dayPart(part)
                .executedAt(LocalDateTime.now())
                .actualDose(actualDose)
                .status("Completed")
                .requires2pAuth(requires2pAuth)
                .secondPersonId(secondPersonUuid)
                .build();
        exec.setCreatedBy(currentUserId);
        exec.setUpdatedBy(currentUserId);
        exec.setExecutedBy(firstPersonUuid);
        exec = executionRepository.save(exec);

        part.setIsCompleted(true);
        part.setNurseName(nurseName);
        part.setUpdatedBy(currentUserId);
        partRepository.save(part);

        log.info("Dose executed: dayPartId={}, firstPersonLogin={}, secondPersonLogin={}",
                dayPartId, currentUserLogin, secondPersonLogin);
        return exec;
    }
}
