package com.superhumans.medicationsheet.service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditChanges;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrescriptionExecutionService {

    PrescriptionExecutionRepository executionRepository;
    PrescriptionDayPartRepository partRepository;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    DomainAuditEmitter auditEmitter;

    @Transactional
    public PrescriptionExecution execute(UUID dayPartId, Long currentUserId, String currentUserLogin, String actualDose, String secondPersonLogin, String secondPersonPassword) {
        PrescriptionDayPart part = partRepository.findById(dayPartId)
                .orElseThrow(() -> new NotFoundException("Day part not found: " + dayPartId));

        UUID firstPersonUuid = UUID.nameUUIDFromBytes(currentUserLogin.getBytes());
        String nurseName = currentUserLogin;

        // Server-owned policy: every dose execution requires two-person
        // authentication with a different nurse's credentials. The client can
        // never opt out of the second-nurse check (audit finding A12).
        User secondPerson = userRepository.findByLogin(secondPersonLogin)
                .orElseThrow(() -> new IllegalArgumentException("Помилка автентифікації другої особи"));

        if (!passwordEncoder.matches(secondPersonPassword == null ? "" : secondPersonPassword, secondPerson.getPasswordHash())) {
            throw new IllegalArgumentException("Невірний пароль другої особи");
        }

        if (secondPerson.getRole() != UserRole.NURSE) {
            throw new IllegalArgumentException("Друга особа повинна мати роль медсестри");
        }

        if (secondPerson.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Друга особа не може бути тією ж, що виконує призначення");
        }

        UUID secondPersonUuid = UUID.nameUUIDFromBytes(secondPersonLogin.getBytes());
        nurseName = currentUserLogin + "/2P:" + secondPersonLogin;

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
        part.setNurseName(nurseName);
        part.setUpdatedBy(currentUserId);
        partRepository.save(part);

        log.info("Dose executed: dayPartId={}", dayPartId);
        final UUID executedDayPartId = dayPartId;
        final Long executedFirstPersonId = currentUserId;
        final Long executedSecondPersonId = secondPerson.getId();
        auditEmitter.emit("medication", () -> AuditEvent.builder()
                .actor(new AuditEvent.AuditActor(
                        AuditEvent.ActorType.USER,
                        executedFirstPersonId == null ? null : executedFirstPersonId.toString(),
                        currentUserLogin,
                        null,
                        java.util.Set.of(),
                        null,
                        new AuditEvent.AuditActor(
                                AuditEvent.ActorType.USER,
                                executedSecondPersonId == null ? null : executedSecondPersonId.toString(),
                                secondPersonLogin,
                                null,
                                java.util.Set.of("NURSE"),
                                null,
                                null)))
                .eventClass(EventClass.BUSINESS)
                .module("medication")
                .functionalArea("dose")
                .action("medication.dose.execute")
                .actionType(ActionType.DOSE_EXECUTE)
                .target(new AuditEvent.AuditTarget("PrescriptionDayPart", executedDayPartId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .changes(List.of(
                        AuditChanges.statusChanged("PLANNED", "COMPLETED"),
                        AuditChanges.fieldChanged("actualDose", DataClass.CLINICAL)))
                .source(AuditEvent.AuditSource.API)
                .build());
        return exec;
    }
}
