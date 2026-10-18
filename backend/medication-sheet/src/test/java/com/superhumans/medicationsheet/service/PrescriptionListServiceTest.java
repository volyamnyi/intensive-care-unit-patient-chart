package com.superhumans.medicationsheet.service;

import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.exception.BusinessException;
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

    @Mock
    private com.superhumans.audit.DomainAuditEmitter auditEmitter;

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
        when(listRepository.findByPatientIdAndDeletedFalseOrderByCreatedAtDescIdDesc(1001L)).thenReturn(List.of(testList));

        var result = service.getByPatient(1001L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientId()).isEqualTo(1001L);
    }

    @Test
    void getByPatient_returnsEmpty_whenNoLists() {
        when(listRepository.findByPatientIdAndDeletedFalseOrderByCreatedAtDescIdDesc(999L)).thenReturn(List.of());

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

// --- acquireLock / releaseLock: deleted (#363) ---
// The methods had no caller in src/main and acquireLock wrote the user id into
// the STATUS column. Concurrent editing is guarded by @Version (see
// PrescriptionRepositoryTest).

    @Test
    void lockMethods_areGone_soTheStatusHackCannotReturn() {
        // #363 tripwire: the dead lock must not come back in its old form -
        // reviving it would re-introduce list.setStatus(userId.toString()).
        assertThat(java.util.Arrays.stream(PrescriptionListService.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName))
                .doesNotContain("acquireLock", "releaseLock");
        assertThat(java.util.Arrays.stream(PrescriptionList.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName))
                .doesNotContain("isEditing", "getEditingUserId", "setEditingUserId");
    }

    // --- close ---

    @Test
    void close_setsFinishedStatus() {
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
        when(listRepository.save(any())).thenReturn(testList);

        service.close(listId);

        verify(listRepository).save(listCaptor.capture());
        assertThat(listCaptor.getValue().getStatus()).isEqualTo("Finished");
    }

    @Test
    void close_whenAlreadyFinished_throwsDocumentLocked() {
        testList.setStatus("Finished");
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));

        assertThatThrownBy(() -> service.close(listId))
                .isInstanceOf(DocumentLockedException.class)
                .hasMessageContaining("already closed");
        verify(listRepository, never()).save(any());
    }

    @Test
    void create_throws_whenPatientHasOpenList() {
        when(listRepository.findByPatientIdAndDeletedFalseOrderByCreatedAtDescIdDesc(1001L)).thenReturn(List.of(testList));

        assertThatThrownBy(() -> service.create(1001L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("активний листок");
        verify(listRepository, never()).save(any());
    }

    @Test
    void create_succeeds_whenOnlyFinishedListExists() {
        testList.setStatus("Finished");
        when(listRepository.findByPatientIdAndDeletedFalseOrderByCreatedAtDescIdDesc(1001L)).thenReturn(List.of(testList));
        when(listRepository.save(any())).thenAnswer(inv -> {
            PrescriptionList l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        var result = service.create(1001L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("Saved");
    }

    @Test
    void getById_whenDeleted_throwsNotFoundException() {
        testList.setDeleted(true);
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));

        assertThatThrownBy(() -> service.getById(listId))
                .isInstanceOf(NotFoundException.class);
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
