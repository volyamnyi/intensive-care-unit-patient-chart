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
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.exception.BusinessException;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.ErrorCode;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionExecutionRepository;
import com.superhumans.repository.core.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public PrescriptionExecution execute(UUID dayPartId, Long currentUserId, String currentUserLogin, String actualDose, String secondPersonLogin, String secondPersonPassword,
            String administeredAtRaw, String dispensedAtRaw) {
        PrescriptionDayPart part = partRepository.findById(dayPartId)
                .orElseThrow(() -> new NotFoundException("Day part not found: " + dayPartId));

        if (Boolean.TRUE.equals(part.getIsCompleted()) || Boolean.TRUE.equals(part.getIsCompletedFinished())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Дозу вже виконано");
        }
        requireNotFinished(part);
        // Only a planned dose can be executed. This guard used to live in
        // PrescriptionItemService.markCompleted, which was removed when the
        // complete endpoint was routed here (#359) - without moving it, the
        // execute path would have become the weaker of the two.
        if (!Boolean.TRUE.equals(part.getIsPlanned())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Незаплановане призначення не може бути виконане");
        }

        // Actual-time facts (#443): parse + validate BEFORE the 2P check - an
        // invalid time must not trigger a second-person authentication attempt,
        // and no state may change on an invalid value (design §5).
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime administeredAt = ActualTimeValidator.parseActualTime(administeredAtRaw);
        LocalDateTime dispensedAt = ActualTimeValidator.parseActualTime(dispensedAtRaw);
        boolean administeredAtManual = administeredAt != null;
        if (administeredAt == null) {
            // Server default: the fact equals the operation time - exactly the
            // semantics the med/030 backfill gives legacy rows.
            administeredAt = now;
        }
        ActualTimeValidator.assertNotInFuture(administeredAt, now);
        ActualTimeValidator.assertNotInFuture(dispensedAt, now);
        // V3 window: [min dayDate of the list; cellDayDate + 1 day] - a night
        // dose of day D may lawfully be administered in the morning of D+1.
        LocalDate minDayDate = minNonDeletedDayDate(part.getDay());
        LocalDate cellDayDate = part.getDay() == null ? null : part.getDay().getDayDate();
        ActualTimeValidator.assertWithinListPeriod(administeredAt, minDayDate, cellDayDate);
        ActualTimeValidator.assertWithinListPeriod(dispensedAt, minDayDate, cellDayDate);
        ActualTimeValidator.assertDispensedNotAfterAdministered(administeredAt, dispensedAt);

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
                .executedAt(now)
                .actualDose(actualDose)
                .status("Completed")
                .requires2pAuth(true)
                .secondPersonId(secondPersonUuid)
                .administeredAt(administeredAt)
                .dispensedAt(dispensedAt)
                .administeredAtManual(administeredAtManual)
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
        // Field-only facts (D4/§B5 - clinical values are never attached):
        // administeredAt only when manually entered, dispensedAt when present.
        List<AuditEvent.AuditChange> changes = new ArrayList<>();
        changes.add(AuditChanges.statusChanged("PLANNED", "COMPLETED"));
        changes.add(AuditChanges.fieldChanged("actualDose", DataClass.CLINICAL));
        if (administeredAtManual) {
            changes.add(AuditChanges.fieldChanged("administeredAt", DataClass.CLINICAL));
        }
        if (dispensedAt != null) {
            changes.add(AuditChanges.fieldChanged("dispensedAt", DataClass.CLINICAL));
        }
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
                .changes(changes)
                .source(AuditEvent.AuditSource.API)
                .build());
        return exec;
    }

    @Transactional
    public PrescriptionExecution updateActualTime(UUID executionId, Long currentUserId, String currentUserLogin,
            String administeredAtRaw, String dispensedAtRaw, String reason, Integer version) {
        PrescriptionExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new NotFoundException("Execution not found: " + executionId));

        PrescriptionDayPart part = execution.getDayPart();
        if (part == null) {
            throw new NotFoundException("Day part not found for execution: " + executionId);
        }
        // A correction only makes sense for an already-executed dose.
        if (!Boolean.TRUE.equals(part.getIsCompleted()) && !Boolean.TRUE.equals(part.getIsCompletedFinished())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Доза не виконана — немає що коригувати");
        }
        requireNotFinished(part);

        // V1-V4 against the same list/day context as the primary fixation.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime administeredAt = ActualTimeValidator.parseActualTime(administeredAtRaw);
        LocalDateTime dispensedAt = ActualTimeValidator.parseActualTime(dispensedAtRaw);
        if (administeredAt == null) {
            // The correction endpoint always carries an explicit value (DTO @NotBlank),
            // but keep the guard defensive: a missing value is a 400, not a silent now().
            throw new IllegalArgumentException("Invalid request value.");
        }
        ActualTimeValidator.assertNotInFuture(administeredAt, now);
        ActualTimeValidator.assertNotInFuture(dispensedAt, now);
        LocalDate minDayDate = minNonDeletedDayDate(part.getDay());
        LocalDate cellDayDate = part.getDay() == null ? null : part.getDay().getDayDate();
        ActualTimeValidator.assertWithinListPeriod(administeredAt, minDayDate, cellDayDate);
        ActualTimeValidator.assertWithinListPeriod(dispensedAt, minDayDate, cellDayDate);
        ActualTimeValidator.assertDispensedNotAfterAdministered(administeredAt, dispensedAt);

        // Optimistic lock: a stale version must not silently overwrite a medically
        // significant value (VersionConflictException -> 409 VERSION_CONFLICT).
        if (version == null || !version.equals(execution.getVersion())) {
            throw new VersionConflictException("Execution version conflict: " + executionId);
        }

        // Capture old values for the audit event BEFORE mutating.
        LocalDateTime oldAdministeredAt = execution.getAdministeredAt();
        LocalDateTime oldDispensedAt = execution.getDispensedAt();

        execution.setAdministeredAt(administeredAt);
        execution.setDispensedAt(dispensedAt);
        execution.setAdministeredAtManual(true);
        // Append-only history in the comment column (full history lives in audit v2
        // + object history). Never overwrite prior corrections.
        String stamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        String entry = "[коригування " + stamp + "] " + currentUserLogin + ": " + reason;
        String existing = execution.getComment();
        execution.setComment(existing == null || existing.isBlank()
                ? entry
                : existing + "\n" + entry);
        execution.setUpdatedBy(currentUserId);
        execution = executionRepository.save(execution);

        log.info("Actual time corrected: executionId={}", executionId);
        final UUID correctedExecutionId = executionId;
        final Long correctedUserId = currentUserId;
        List<AuditEvent.AuditChange> changes = new ArrayList<>();
        changes.add(new AuditEvent.AuditChange(
                "administeredAt", AuditEvent.ChangeType.SET, DataClass.IDENTIFIER,
                oldAdministeredAt == null ? null : oldAdministeredAt.toString(),
                administeredAt.toString()));
        if (oldDispensedAt != null || dispensedAt != null) {
            changes.add(new AuditEvent.AuditChange(
                    "dispensedAt", AuditEvent.ChangeType.SET, DataClass.IDENTIFIER,
                    oldDispensedAt == null ? null : oldDispensedAt.toString(),
                    dispensedAt == null ? null : dispensedAt.toString()));
        }
        changes.add(AuditChanges.fieldChanged("reason", DataClass.NARRATIVE));
        auditEmitter.emit("medication", () -> AuditEvent.builder()
                .actor(new AuditEvent.AuditActor(
                        AuditEvent.ActorType.USER,
                        correctedUserId == null ? null : correctedUserId.toString(),
                        currentUserLogin,
                        null,
                        java.util.Set.of(),
                        null,
                        null))
                .eventClass(EventClass.BUSINESS)
                .module("medication")
                .functionalArea("dose")
                .action("medication.dose.actual_time.update")
                .actionType(ActionType.UPDATE)
                .target(new AuditEvent.AuditTarget("PrescriptionExecution", correctedExecutionId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .changes(changes)
                .source(AuditEvent.AuditSource.API)
                .build());
        return execution;
    }

    /**
     * Latest execution per day part, batched (#443): the grid reads one
     * execution row per part, so a per-part query would be N+1. "Latest" =
     * newest {@code createdAt}, id as the deterministic tiebreak.
     */
    @Transactional(readOnly = true)
    public Map<UUID, PrescriptionExecution> latestByDayPartIds(Collection<UUID> dayPartIds) {
        if (dayPartIds == null || dayPartIds.isEmpty()) {
            return Map.of();
        }
        List<PrescriptionExecution> executions = new ArrayList<>(
                executionRepository.findByDayPartIdIn(dayPartIds));
        executions.sort(Comparator
                .comparing(PrescriptionExecution::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(PrescriptionExecution::getId, Comparator.nullsFirst(Comparator.naturalOrder())));
        Map<UUID, PrescriptionExecution> latest = new HashMap<>();
        for (PrescriptionExecution exec : executions) {
            if (exec.getDayPart() != null && exec.getDayPart().getId() != null) {
                // Ascending sort + later overwrite = newest per part.
                latest.put(exec.getDayPart().getId(), exec);
            }
        }
        return latest;
    }

    /** Min dayDate over non-deleted days of the item (V3 lower bound); null when unknown. */
    private static LocalDate minNonDeletedDayDate(PrescriptionItemDay partDay) {
        if (partDay == null || partDay.getItem() == null || partDay.getItem().getDays() == null) {
            return null;
        }
        return partDay.getItem().getDays().stream()
                .filter(day -> !Boolean.TRUE.equals(day.getDeleted()))
                .map(PrescriptionItemDay::getDayDate)
                .filter(java.util.Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    /** A closed ("Finished") medication sheet is immutable — mirrors the ICU day lock. */
    private static void requireNotFinished(PrescriptionDayPart part) {
        PrescriptionItemDay day = part.getDay();
        if (day == null || day.getItem() == null) {
            return;
        }
        PrescriptionItem item = day.getItem();
        PrescriptionList list = item.getList();
        if (list != null && list.isFinished()) {
            throw new DocumentLockedException("Листок завершено — зміни заборонені");
        }
    }
}
