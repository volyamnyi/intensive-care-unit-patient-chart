package com.superhumans.service;

import com.superhumans.dto.MedicalNoteCreateRequest;
import com.superhumans.dto.MedicalNotePatchRequest;
import com.superhumans.dto.MedicalNoteResponse;
import com.superhumans.entity.*;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.MedicalNoteRepository;
import com.superhumans.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalNoteServiceTest {

    @Mock
    private MedicalNoteRepository medicalNoteRepository;

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private MedicalNoteService medicalNoteService;

    @Captor
    private ArgumentCaptor<MedicalNote> noteCaptor;

    private UUID noteId;
    private UUID clinicalDayId;
    private UUID userId;
    private ClinicalDay clinicalDay;
    private User user;

    @BeforeEach
    void setUp() {
        noteId = UUID.randomUUID();
        clinicalDayId = UUID.randomUUID();
        userId = UUID.randomUUID();
        clinicalDay = ClinicalDay.builder()
                .status(ClinicalDayStatus.OPEN)
                .build();
        clinicalDay.setId(clinicalDayId);
        clinicalDay.setVersion(0);
        user = User.builder()
                .login("doctor1")
                .role(UserRole.DOCTOR)
                .build();
        user.setId(userId);
    }

    @Test
    void getNote_whenFound_returnsResponse() {
        MedicalNote note = MedicalNote.builder()
                .text("Test note")
                .build();
        note.setId(noteId);
        note.setClinicalDay(clinicalDay);
        when(medicalNoteRepository.findById(noteId)).thenReturn(Optional.of(note));

        MedicalNoteResponse res = medicalNoteService.getNote(noteId);

        assertThat(res.getId()).isEqualTo(noteId);
        assertThat(res.getText()).isEqualTo("Test note");
    }

    @Test
    void getNote_whenNotFound_throws() {
        when(medicalNoteRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalNoteService.getNote(noteId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getNotesByClinicalDay_returnsList() {
        MedicalNote note = MedicalNote.builder().build();
        note.setId(noteId);
        note.setClinicalDay(clinicalDay);
        when(medicalNoteRepository.findByClinicalDayIdOrderByCreatedAtAsc(clinicalDayId))
                .thenReturn(List.of(note));

        List<MedicalNoteResponse> results = medicalNoteService.getNotesByClinicalDay(clinicalDayId);

        assertThat(results).hasSize(1);
    }

    @Test
    void createNote_createsSuccessfully() {
        MedicalNoteCreateRequest req = new MedicalNoteCreateRequest("OBSERVATION", "Patient is stable");

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        MedicalNote saved = MedicalNote.builder().build();
        saved.setId(noteId);
        saved.setClinicalDay(clinicalDay);
        saved.setVersion(0);
        when(medicalNoteRepository.save(any(MedicalNote.class))).thenReturn(saved);

        MedicalNoteResponse res = medicalNoteService.createNote(clinicalDayId, req, userId);

        verify(medicalNoteRepository).save(noteCaptor.capture());
        assertThat(noteCaptor.getValue().getNoteType()).isEqualTo("OBSERVATION");
        assertThat(noteCaptor.getValue().getText()).isEqualTo("Patient is stable");
        assertThat(noteCaptor.getValue().getRole()).isEqualTo("DOCTOR");
        assertThat(noteCaptor.getValue().getAuthorId()).isEqualTo(userId);
        verify(auditService).logCreate("MedicalNote", noteId, userId);
    }

    @Test
    void createNote_whenDaySigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        MedicalNoteCreateRequest req = new MedicalNoteCreateRequest("OBS", "text");

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));

        assertThatThrownBy(() -> medicalNoteService.createNote(clinicalDayId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void updateNote_updatesSuccessfully() {
        MedicalNote existing = MedicalNote.builder()
                .text("Original")
                .build();
        existing.setId(noteId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);

        MedicalNotePatchRequest req = new MedicalNotePatchRequest("Updated text", 0);

        when(medicalNoteRepository.findById(noteId)).thenReturn(Optional.of(existing));
        MedicalNote saved = MedicalNote.builder().build();
        saved.setId(noteId);
        saved.setClinicalDay(clinicalDay);
        saved.setVersion(1);
        when(medicalNoteRepository.save(any(MedicalNote.class))).thenReturn(saved);

        MedicalNoteResponse res = medicalNoteService.updateNote(noteId, req, userId);

        verify(medicalNoteRepository).save(noteCaptor.capture());
        assertThat(noteCaptor.getValue().getText()).isEqualTo("Updated text");
        verify(auditService).logUpdate("MedicalNote", noteId, userId, null, "Updated note text");
    }

    @Test
    void updateNote_withVersionMismatch_throws() {
        MedicalNote existing = MedicalNote.builder().build();
        existing.setId(noteId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);
        MedicalNotePatchRequest req = new MedicalNotePatchRequest("text", 999);

        when(medicalNoteRepository.findById(noteId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> medicalNoteService.updateNote(noteId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }
}
