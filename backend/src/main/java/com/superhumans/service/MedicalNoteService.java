package com.superhumans.service;

import com.superhumans.dto.MedicalNoteCreateRequest;
import com.superhumans.dto.MedicalNotePatchRequest;
import com.superhumans.dto.MedicalNoteResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.MedicalNote;
import com.superhumans.entity.User;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.MedicalNoteMapper;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.MedicalNoteRepository;
import com.superhumans.repository.UserRepository;
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

    public MedicalNoteResponse getNote(UUID id) {
        MedicalNote note = medicalNoteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical note not found: " + id));
        return MedicalNoteMapper.toResponse(note);
    }

    public List<MedicalNoteResponse> getNotesByClinicalDay(UUID clinicalDayId) {
        return medicalNoteRepository.findByClinicalDayIdOrderByCreatedAtAsc(clinicalDayId)
                .stream().map(MedicalNoteMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public MedicalNoteResponse createNote(UUID clinicalDayId, MedicalNoteCreateRequest request, UUID userId) {
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
        return MedicalNoteMapper.toResponse(note);
    }

    @Transactional
    public MedicalNoteResponse updateNote(UUID id, MedicalNotePatchRequest request, UUID userId) {
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
        return MedicalNoteMapper.toResponse(note);
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }
}
