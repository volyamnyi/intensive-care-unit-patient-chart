package com.superhumans.medicationsheet.service;

import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
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
class PrescriptionListServiceTest {

    @Mock
    private PrescriptionListRepository listRepository;

    @InjectMocks
    private PrescriptionListService service;

    @Captor
    private ArgumentCaptor<PrescriptionList> listCaptor;

    private UUID listId;
    private PrescriptionList testList;

    @BeforeEach
    void setUp() {
        listId = UUID.randomUUID();
        testList = PrescriptionList.builder()
                .patientId(1001L)
                .documentName("Test Document")
                .status("Saved")
                .build();
        testList.setId(listId);
        testList.setVersion(0);
    }

    // --- getByPatient ---

    @Test
    void getByPatient_returnsLists() {
        when(listRepository.findByPatientIdAndDeletedFalse(1001L)).thenReturn(List.of(testList));

        var result = service.getByPatient(1001L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientId()).isEqualTo(1001L);
    }

    @Test
    void getByPatient_returnsEmpty_whenNoLists() {
        when(listRepository.findByPatientIdAndDeletedFalse(999L)).thenReturn(List.of());

        assertThat(service.getByPatient(999L)).isEmpty();
    }

    // --- getById ---

    @Test
    void getById_whenFound_returnsList() {
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));

        PrescriptionList result = service.getById(listId);

        assertThat(result.getId()).isEqualTo(listId);
        assertThat(result.getDocumentName()).isEqualTo("Test Document");
    }

    @Test
    void getById_whenNotFound_throwsNotFoundException() {
        when(listRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Prescription list not found");
    }

    // --- create ---

    @Test
    void create_setsPatientIdAndDefaultStatus() {
        PrescriptionList saved = PrescriptionList.builder()
                .patientId(2002L)
                .status("Saved")
                .documentName("\u041B\u0438\u0441\u0442\u043E\u043A \u043B\u0456\u043A\u0430\u0440\u0441\u044C\u043A\u0438\u0445 \u043F\u0440\u0438\u0437\u043D\u0430\u0447\u0435\u043D\u044C")
                .build();
        saved.setId(listId);
        when(listRepository.save(any(PrescriptionList.class))).thenReturn(saved);

        PrescriptionList result = service.create(2002L);

        verify(listRepository).save(listCaptor.capture());
        PrescriptionList captured = listCaptor.getValue();
        assertThat(captured.getPatientId()).isEqualTo(2002L);
        assertThat(captured.getStatus()).isEqualTo("Saved");
        assertThat(result.getId()).isEqualTo(listId);
    }

    // --- update ---

    @Test
    void update_changesDocumentName() {
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
        when(listRepository.save(any(PrescriptionList.class))).thenReturn(testList);

        service.update(listId, "New Name");

        verify(listRepository).save(listCaptor.capture());
        assertThat(listCaptor.getValue().getDocumentName()).isEqualTo("New Name");
    }

    @Test
    void update_nullName_keepsOriginal() {
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
        when(listRepository.save(any(PrescriptionList.class))).thenReturn(testList);

        service.update(listId, null);

        verify(listRepository).save(listCaptor.capture());
        assertThat(listCaptor.getValue().getDocumentName()).isEqualTo("Test Document");
    }

    // --- acquireLock ---

    @Test
    void acquireLock_setsEditingUser() {
        UUID userId = UUID.randomUUID();
        testList.setStatus("Saved");

        service.acquireLock(testList, userId);

        assertThat(testList.getEditingUserId()).isEqualTo(userId);
        assertThat(testList.getEditingStartedAt()).isNotNull();
    }

    @Test
    void acquireLock_throws_whenFinished() {
        testList.setStatus("Finished");
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.acquireLock(testList, userId))
                .isInstanceOf(DocumentLockedException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void acquireLock_throws_whenEditedByAnotherUser() {
        UUID otherUser = UUID.randomUUID();
        UUID currentUser = UUID.randomUUID();
        testList.setEditingUserId(otherUser);
        testList.setStatus(otherUser.toString());

        assertThatThrownBy(() -> service.acquireLock(testList, currentUser))
                .isInstanceOf(DocumentLockedException.class)
                .hasMessageContaining("another user");
    }

    @Test
    void acquireLock_succeeds_whenSameUserReacquires() {
        UUID userId = UUID.randomUUID();
        testList.setEditingUserId(userId);
        testList.setStatus(userId.toString());

        service.acquireLock(testList, userId);

        assertThat(testList.getEditingUserId()).isEqualTo(userId);
    }

    // --- releaseLock ---

    @Test
    void releaseLock_clearsEditingUser_whenSameUser() {
        UUID userId = UUID.randomUUID();
        testList.setEditingUserId(userId);
        testList.setStatus(userId.toString());
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
        when(listRepository.save(any())).thenReturn(testList);

        service.releaseLock(listId, userId);

        verify(listRepository).save(listCaptor.capture());
        assertThat(listCaptor.getValue().getEditingUserId()).isNull();
        assertThat(listCaptor.getValue().getStatus()).isEqualTo("Saved");
    }

    @Test
    void releaseLock_noop_whenDifferentUser() {
        UUID otherUser = UUID.randomUUID();
        UUID currentUser = UUID.randomUUID();
        testList.setEditingUserId(otherUser);
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));

        service.releaseLock(listId, currentUser);

        verify(listRepository, never()).save(any());
    }

    // --- close ---

    @Test
    void close_setsFinishedStatus() {
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
        when(listRepository.save(any())).thenReturn(testList);

        service.close(listId);

        verify(listRepository).save(listCaptor.capture());
        assertThat(listCaptor.getValue().getStatus()).isEqualTo("Finished");
        assertThat(listCaptor.getValue().getEditingUserId()).isNull();
        assertThat(listCaptor.getValue().getEditingStartedAt()).isNull();
    }

    // --- delete ---

    @Test
    void delete_softDeletesList() {
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
        when(listRepository.save(any())).thenReturn(testList);

        service.delete(listId);

        verify(listRepository).save(listCaptor.capture());
        assertThat(listCaptor.getValue().getDeleted()).isTrue();
    }
}
