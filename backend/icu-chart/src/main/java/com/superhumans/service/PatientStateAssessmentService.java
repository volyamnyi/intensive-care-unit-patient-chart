package com.superhumans.service;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditChanges;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.dto.PatientStateCreateRequest;
import com.superhumans.dto.PatientStatePatchRequest;
import com.superhumans.dto.PatientStateResponse;
import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.ClinicalDayStatus;
import com.superhumans.icu.entity.PatientStateAssessment;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.PatientStateMapper;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.PatientStateAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatientStateAssessmentService {

    PatientStateAssessmentRepository patientStateRepository;
    ClinicalDayRepository clinicalDayRepository;
    AuditService auditService;
    DomainAuditEmitter auditEmitter;
    PatientStateMapper patientStateMapper;

    public List<PatientStateResponse> getByClinicalDay(UUID clinicalDayId) {
        return patientStateRepository.findByClinicalDayIdOrderByRecordHourAsc(clinicalDayId)
                .stream().map(patientStateMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PatientStateResponse create(UUID clinicalDayId, PatientStateCreateRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + clinicalDayId));
        assertNotLocked(day);

        PatientStateAssessment entity = PatientStateAssessment.builder()
                .clinicalDay(day)
                .recordHour(request.getRecordHour())
                .consciousness(request.getConsciousness())
                .skin(request.getSkin())
                .edema(request.getEdema())
                .mucousMembranes(request.getMucousMembranes())
                .peripheralCirculation(request.getPeripheralCirculation())
                .bowelSounds(request.getBowelSounds())
                .generalCondition(request.getGeneralCondition())
                .additionalNotes(request.getAdditionalNotes())
                .build();
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity = patientStateRepository.save(entity);
        auditService.logCreate("PatientStateAssessment", entity.getId(), userId);
        final UUID createdStateId = entity.getId();
        auditEmitter.emit("icu", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("patient-state")
                .action("icu.patient_state.create")
                .actionType(ActionType.CREATE)
                .target(new AuditEvent.AuditTarget("PatientStateAssessment", createdStateId.toString(), null))
                .parentTarget(new AuditEvent.AuditTarget("ClinicalDay", clinicalDayId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .changes(List.of(AuditChanges.fieldChanged("stateFields", DataClass.CLINICAL)))
                .source(AuditEvent.AuditSource.API)
                .build());
        return patientStateMapper.toResponse(entity);
    }

    @Transactional
    public PatientStateResponse update(UUID id, PatientStatePatchRequest request, Long userId) {
        PatientStateAssessment entity = patientStateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient state assessment not found: " + id));

        if (!entity.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Patient state assessment was modified by another user");
        }
        assertNotLocked(entity.getClinicalDay());

        if (request.getConsciousness() != null) entity.setConsciousness(request.getConsciousness());
        if (request.getSkin() != null) entity.setSkin(request.getSkin());
        if (request.getEdema() != null) entity.setEdema(request.getEdema());
        if (request.getMucousMembranes() != null) entity.setMucousMembranes(request.getMucousMembranes());
        if (request.getPeripheralCirculation() != null) entity.setPeripheralCirculation(request.getPeripheralCirculation());
        if (request.getBowelSounds() != null) entity.setBowelSounds(request.getBowelSounds());
        if (request.getGeneralCondition() != null) entity.setGeneralCondition(request.getGeneralCondition());
        final boolean stateNotesChanged = request.getAdditionalNotes() != null;
        if (stateNotesChanged) entity.setAdditionalNotes(request.getAdditionalNotes());
        entity.setUpdatedBy(userId);
        entity = patientStateRepository.save(entity);
        auditService.logUpdate("PatientStateAssessment", id, userId, null, "Updated assessment");
        auditEmitter.emit("icu", () -> {
            java.util.List<AuditEvent.AuditChange> changes = new java.util.ArrayList<>();
            changes.add(AuditChanges.fieldChanged("stateFields", DataClass.CLINICAL));
            if (stateNotesChanged) {
                changes.add(AuditChanges.fieldChanged("additionalNotes", DataClass.NARRATIVE));
            }
            return AuditEvent.builder()
                    .actor(AuditActorResolver.fromCurrentContext())
                    .eventClass(EventClass.BUSINESS)
                    .module("icu")
                    .functionalArea("patient-state")
                    .action("icu.patient_state.update")
                    .actionType(ActionType.UPDATE)
                    .target(new AuditEvent.AuditTarget("PatientStateAssessment", id.toString(), null))
                    .outcome(AuditEvent.AuditOutcome.SUCCESS)
                    .changes(changes)
                    .source(AuditEvent.AuditSource.API)
                    .build();
        });
        return patientStateMapper.toResponse(entity);
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.NURSE_SIGNED
                || day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }
}
