package com.superhumans.service;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditChanges;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.dto.MedicalNoteCreateRequest;
import com.superhumans.dto.MedicalNotePatchRequest;
import com.superhumans.dto.MedicalNoteResponse;
import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.ClinicalDayStatus;
import com.superhumans.icu.entity.MedicalNote;
import com.superhumans.entity.core.User;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.MedicalNoteMapper;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.MedicalNoteRepository;
import com.superhumans.repository.core.UserRepository;
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
public class MedicalNoteService {

    MedicalNoteRepository medicalNoteRepository;
    ClinicalDayRepository clinicalDayRepository;
    UserRepository userRepository;
    AuditService auditService;
    DomainAuditEmitter auditEmitter;
    MedicalNoteMapper medicalNoteMapper;

    public MedicalNoteResponse getNote(UUID id) {
        MedicalNote note = medicalNoteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical note not found: " + id));
        return medicalNoteMapper.toResponse(note);
    }

    public List<MedicalNoteResponse> getNotesByClinicalDay(UUID clinicalDayId) {
        return medicalNoteRepository.findByClinicalDayIdOrderByCreatedAtAsc(clinicalDayId)
                .stream().map(medicalNoteMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public MedicalNoteResponse createNote(UUID clinicalDayId, MedicalNoteCreateRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + clinicalDayId));
        assertNotLocked(day);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        MedicalNote note = MedicalNote.builder()
                .clinicalDay(day)
                .authorId(userId)
                .role(user.getRole().name())
                .noteType(request.getNoteType())
                .text(request.getText())
                .build();
        note.setCreatedBy(userId);
        note.setUpdatedBy(userId);
        note = medicalNoteRepository.save(note);
        auditService.logCreate("MedicalNote", note.getId(), userId);
        final UUID createdNoteId = note.getId();
        auditEmitter.emit("icu", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("medical-note")
                .action("icu.medical_note.create")
                .actionType(ActionType.CREATE)
                .target(new AuditEvent.AuditTarget("MedicalNote", createdNoteId.toString(), null))
                .parentTarget(new AuditEvent.AuditTarget("ClinicalDay", clinicalDayId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .changes(List.of(AuditChanges.fieldChanged("noteText", DataClass.NARRATIVE)))
                .source(AuditEvent.AuditSource.API)
                .build());
        return medicalNoteMapper.toResponse(note);
    }

    @Transactional
    public MedicalNoteResponse updateNote(UUID id, MedicalNotePatchRequest request, Long userId) {
        MedicalNote note = medicalNoteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical note not found: " + id));

        if (!note.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Medical note was modified by another user");
        }
        assertNotLocked(note.getClinicalDay());

        if (request.getText() != null) note.setText(request.getText());
        note.setUpdatedBy(userId);
        note = medicalNoteRepository.save(note);
        auditService.logUpdate("MedicalNote", id, userId, null, "Updated note text");
        final boolean textChanged = request.getText() != null;
        auditEmitter.emit("icu", () -> {
            java.util.List<AuditEvent.AuditChange> changes = new java.util.ArrayList<>();
            if (textChanged) {
                changes.add(AuditChanges.fieldChanged("noteText", DataClass.NARRATIVE));
            }
            return AuditEvent.builder()
                    .actor(AuditActorResolver.fromCurrentContext())
                    .eventClass(EventClass.BUSINESS)
                    .module("icu")
                    .functionalArea("medical-note")
                    .action("icu.medical_note.update")
                    .actionType(ActionType.UPDATE)
                    .target(new AuditEvent.AuditTarget("MedicalNote", id.toString(), null))
                    .outcome(AuditEvent.AuditOutcome.SUCCESS)
                    .changes(changes)
                    .source(AuditEvent.AuditSource.API)
                    .build();
        });
        return medicalNoteMapper.toResponse(note);
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }
}
