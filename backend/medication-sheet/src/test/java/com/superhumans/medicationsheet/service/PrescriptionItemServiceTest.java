package com.superhumans.medicationsheet.service;

import com.superhumans.medicationsheet.entity.*;
import com.superhumans.exception.BusinessException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.repository.*;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionItemServiceTest {

    @Mock private PrescriptionItemRepository itemRepository;
    @Mock private PrescriptionItemDayRepository dayRepository;
    @Mock private PrescriptionDayPartRepository partRepository;
    @Mock private PrescriptionListRepository listRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private PrescriptionItemService service;

    @Captor private ArgumentCaptor<PrescriptionItem> itemCaptor;
    @Captor private ArgumentCaptor<PrescriptionDayPart> partCaptor;

    private UUID listId;
    private UUID itemId;
    private PrescriptionList testList;
    private PrescriptionItem testItem;

    @BeforeEach
    void setUp() {
        listId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        testList = PrescriptionList.builder().patientId(1001L).status("Saved").build();
        testList.setId(listId);
        testItem = PrescriptionItem.builder()
                .list(testList)
                .medicineName("Dopamine")
                .medicineMethod("IV")
                .regime("stat")
                .status("Active")
                .sortOrder(0)
                .build();
        testItem.setId(itemId);
    }

    // --- getByList ---

    @Test
    void getByList_returnsItems() {
        when(itemRepository.findByListIdAndDeletedFalseOrderBySortOrderAsc(listId)).thenReturn(List.of(testItem));

        var result = service.getByList(listId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMedicineName()).isEqualTo("Dopamine");
    }

    @Test
    void getByList_returnsEmpty() {
        when(itemRepository.findByListIdAndDeletedFalseOrderBySortOrderAsc(listId)).thenReturn(List.of());

        assertThat(service.getByList(listId)).isEmpty();
    }

    // --- addItem ---

    @Test
    void addItem_createsItemWith21DaysAnd4PartsEach() {
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
        when(itemRepository.findByListIdAndDeletedFalseOrderBySortOrderAsc(listId)).thenReturn(List.of());
        when(itemRepository.save(any(PrescriptionItem.class))).thenReturn(testItem);
        when(dayRepository.save(any(PrescriptionItemDay.class))).thenAnswer(inv -> {
            PrescriptionItemDay d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(partRepository.save(any(PrescriptionDayPart.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionItem result = service.addItem(listId, "Aspirin", "PO", "BID");

        // 1 item saved
        verify(itemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getMedicineName()).isEqualTo("Aspirin");
        assertThat(itemCaptor.getValue().getStatus()).isEqualTo("Active");

        // 21 days created
        verify(dayRepository, times(21)).save(any(PrescriptionItemDay.class));

        // 84 day parts created (21 days × 4 parts: morning, day, evening, night)
        verify(partRepository, times(84)).save(partCaptor.capture());

        assertThat(result.getId()).isEqualTo(itemId);
    }

    @Test
    void addItem_setsSortOrderFromExistingCount() {
        PrescriptionItem existing = PrescriptionItem.builder().medicineName("Existing").build();
        existing.setId(UUID.randomUUID());
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
        when(itemRepository.findByListIdAndDeletedFalseOrderBySortOrderAsc(listId)).thenReturn(List.of(existing));
        when(itemRepository.save(any(PrescriptionItem.class))).thenReturn(testItem);
        when(dayRepository.save(any())).thenAnswer(inv -> { PrescriptionItemDay d = inv.getArgument(0); d.setId(UUID.randomUUID()); return d; });
        when(partRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addItem(listId, "NewDrug", "IV", "stat");

        verify(itemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getSortOrder()).isEqualTo(1);
    }

    @Test
    void addItem_throws_whenListNotFound() {
        when(listRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItem(UUID.randomUUID(), "Drug", "PO", "BID"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addItem_createsDayPartsForAllFourPeriods() {
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
        when(itemRepository.findByListIdAndDeletedFalseOrderBySortOrderAsc(listId)).thenReturn(List.of());
        when(itemRepository.save(any())).thenReturn(testItem);
        when(dayRepository.save(any())).thenAnswer(inv -> { PrescriptionItemDay d = inv.getArgument(0); d.setId(UUID.randomUUID()); return d; });
        when(partRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addItem(listId, "Drug", "PO", "BID");

        verify(partRepository, times(84)).save(partCaptor.capture());
        List<PrescriptionDayPart> allParts = new java.util.ArrayList<>();
        for (var inv : org.mockito.Mockito.mockingDetails(partRepository).getInvocations()) {
            if (inv.getMethod().getName().equals("save")) {
                allParts.add(inv.getArgument(0));
            }
        }
        long morningCount = allParts.stream().filter(p -> "morning".equals(p.getPeriod())).count();
        long nightCount = allParts.stream().filter(p -> "night".equals(p.getPeriod())).count();
        assertThat(morningCount).isEqualTo(21);
        assertThat(nightCount).isEqualTo(21);
    }

    // --- addDay ---

    @Test
    void addDay_createsDayAfterLastExistingWithFourParts() {
        PrescriptionItemDay first = dayWithDate(LocalDate.of(2026, 1, 1));
        PrescriptionItemDay last = dayWithDate(LocalDate.of(2026, 1, 2));
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(testItem));
        when(dayRepository.findByItemIdAndDeletedFalseOrderByDayDateAsc(itemId))
                .thenReturn(List.of(first, last));
        when(dayRepository.save(any(PrescriptionItemDay.class))).thenAnswer(inv -> {
            PrescriptionItemDay d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(partRepository.save(any(PrescriptionDayPart.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionItemDay result = service.addDay(itemId);

        assertThat(result.getDayDate()).isEqualTo(LocalDate.of(2026, 1, 3));
        verify(itemRepository).findByIdForUpdate(itemId);
        verify(dayRepository).save(any(PrescriptionItemDay.class));
        verify(partRepository, times(4)).save(partCaptor.capture());
        List<String> periods = partCaptor.getAllValues().stream().map(PrescriptionDayPart::getPeriod).toList();
        assertThat(periods).containsExactlyInAnyOrder("morning", "day", "evening", "night");
        for (PrescriptionDayPart part : partCaptor.getAllValues()) {
            assertThat(part.getIsPlanned()).isFalse();
            assertThat(part.getIsPlannedFinished()).isFalse();
            assertThat(part.getIsCompleted()).isFalse();
            assertThat(part.getIsCompletedFinished()).isFalse();
        }
    }

    @Test
    void addDay_whenItemHasNoActiveDays_createsForToday() {
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(testItem));
        when(dayRepository.findByItemIdAndDeletedFalseOrderByDayDateAsc(itemId)).thenReturn(List.of());
        when(dayRepository.save(any())).thenAnswer(inv -> { PrescriptionItemDay d = inv.getArgument(0); d.setId(UUID.randomUUID()); return d; });
        when(partRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionItemDay result = service.addDay(itemId);

        assertThat(result.getDayDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void addDay_throws_whenItemNotFound() {
        when(itemRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addDay(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addDay_throws_whenItemDeleted() {
        testItem.setDeleted(true);
        when(itemRepository.findByIdForUpdate(itemId)).thenReturn(Optional.of(testItem));

        assertThatThrownBy(() -> service.addDay(itemId))
                .isInstanceOf(NotFoundException.class);
    }

    // --- removeDay ---

    @Test
    void removeDay_softDeletesDay() {
        PrescriptionItemDay day = dayWithParts(LocalDate.of(2026, 1, 5),
                buildPart("morning", false, false), buildPart("evening", false, false));
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(dayRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.removeDay(itemId, day.getId());

        verify(dayRepository).save(argThat(d -> Boolean.TRUE.equals(d.getDeleted())));
    }

    @Test
    void removeDay_throws_whenDayHasCompletedPart() {
        PrescriptionItemDay day = dayWithParts(LocalDate.of(2026, 1, 5),
                buildPart("morning", true, false), buildPart("evening", false, false));
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));

        assertThatThrownBy(() -> service.removeDay(itemId, day.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("День містить виконані призначення, видалення неможливе");
        verify(dayRepository, never()).save(any());
    }

    @Test
    void removeDay_throws_whenDayHasCompletedFinishedPart() {
        PrescriptionItemDay day = dayWithParts(LocalDate.of(2026, 1, 5),
                buildPart("morning", false, false), buildPart("night", false, true));
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));

        assertThatThrownBy(() -> service.removeDay(itemId, day.getId()))
                .isInstanceOf(BusinessException.class);
        verify(dayRepository, never()).save(any());
    }

    @Test
    void removeDay_throws_whenDayNotBelongingToItem() {
        PrescriptionItem otherItem = PrescriptionItem.builder().build();
        otherItem.setId(UUID.randomUUID());
        PrescriptionItemDay day = dayWithParts(LocalDate.of(2026, 1, 5), buildPart("morning", false, false));
        day.setItem(otherItem);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));

        assertThatThrownBy(() -> service.removeDay(itemId, day.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeDay_throws_whenDayAlreadyDeleted() {
        PrescriptionItemDay day = dayWithParts(LocalDate.of(2026, 1, 5), buildPart("morning", false, false));
        day.setDeleted(true);
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));

        assertThatThrownBy(() -> service.removeDay(itemId, day.getId()))
                .isInstanceOf(NotFoundException.class);
        verify(dayRepository, never()).save(any());
    }

    @Test
    void removeDay_throws_whenDayNotFound() {
        when(dayRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeDay(itemId, UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    // --- removeItem ---

    @Test
    void removeItem_softDeletesItem() {
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any())).thenReturn(testItem);

        service.removeItem(itemId);

        verify(itemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getDeleted()).isTrue();
    }

    @Test
    void removeItem_throws_whenNotFound() {
        when(itemRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeItem(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    // --- planDose ---

    @Test
    void planDose_setsDoseAndDoctorName() {
        UUID dayPartId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Long userId = 7L;
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").isPlanned(false).isCompleted(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));
        when(partRepository.save(any())).thenReturn(part);

        PrescriptionDayPart result = service.planDose(dayPartId, "50mg", doctorId, userId);

        verify(partRepository).save(partCaptor.capture());
        assertThat(partCaptor.getValue().getDose()).isEqualTo("50mg");
        assertThat(partCaptor.getValue().getIsPlanned()).isTrue();
        assertThat(partCaptor.getValue().getIsPlannedFinished()).isFalse();
        assertThat(partCaptor.getValue().getDoctorName()).isEqualTo(doctorId.toString());
        verify(auditService).logAction("PrescriptionDayPart", dayPartId, "PLAN", userId);
    }

    @Test
    void planDose_resetsPlannedFinished_whenReplanningCancelled() {
        UUID dayPartId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Long userId = 7L;
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(true).isCompleted(false).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));
        when(partRepository.save(any())).thenReturn(part);

        PrescriptionDayPart result = service.planDose(dayPartId, "60mg", doctorId, userId);

        assertThat(result.getIsPlanned()).isTrue();
        assertThat(result.getIsPlannedFinished()).isFalse();
        assertThat(result.getDose()).isEqualTo("60mg");
        verify(auditService).logAction("PrescriptionDayPart", dayPartId, "PLAN", userId);
    }

    @Test
    void planDose_throws_whenNotFound() {
        when(partRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.planDose(UUID.randomUUID(), "10mg", UUID.randomUUID(), 7L))
                .isInstanceOf(NotFoundException.class);
    }

    // --- markCompleted ---

    @Test
    void markCompleted_setsNurseNameAndFlag() {
        UUID dayPartId = UUID.randomUUID();
        UUID nurseId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").isPlanned(true).isCompleted(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));
        when(partRepository.save(any())).thenReturn(part);

        service.markCompleted(dayPartId, nurseId);

        verify(partRepository).save(partCaptor.capture());
        assertThat(partCaptor.getValue().getIsCompleted()).isTrue();
        assertThat(partCaptor.getValue().getNurseName()).isEqualTo(nurseId.toString());
    }

    // --- markCompletedFinished ---

    @Test
    void markCompletedFinished_setsFlag() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").isCompleted(true).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));
        when(partRepository.save(any())).thenReturn(part);

        service.markCompletedFinished(dayPartId);

        verify(partRepository).save(partCaptor.capture());
        assertThat(partCaptor.getValue().getIsCompletedFinished()).isTrue();
    }

    // --- markPlannedFinished («Відмінити препарат») ---

    @Test
    void markPlannedFinished_setsFlagKeepsDoseAndAudits() {
        UUID dayPartId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Long userId = 7L;
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(false).isCompleted(false).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));
        when(partRepository.save(any())).thenReturn(part);

        PrescriptionDayPart result = service.markPlannedFinished(dayPartId, doctorId, userId);

        assertThat(result.getIsPlanned()).isTrue();
        assertThat(result.getIsPlannedFinished()).isTrue();
        assertThat(result.getDose()).isEqualTo("50mg");
        verify(auditService).logAction("PrescriptionDayPart", dayPartId, "CANCEL", userId);
    }

    @Test
    void markPlannedFinished_throws_whenCompleted() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(false).isCompleted(true).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> service.markPlannedFinished(dayPartId, UUID.randomUUID(), 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Виконане призначення не може бути відмінене");
        verify(partRepository, never()).save(any());
        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void markPlannedFinished_throws_whenCompletedFinished() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(false).isCompleted(true).isCompletedFinished(true).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> service.markPlannedFinished(dayPartId, UUID.randomUUID(), 7L))
                .isInstanceOf(BusinessException.class);
        verify(partRepository, never()).save(any());
    }

    @Test
    void markPlannedFinished_throws_whenUnplanned() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning")
                .isPlanned(false).isPlannedFinished(false).isCompleted(false).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> service.markPlannedFinished(dayPartId, UUID.randomUUID(), 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Незаплановане призначення не може бути відмінене");
        verify(partRepository, never()).save(any());
    }

    @Test
    void markPlannedFinished_throws_whenAlreadyCancelled() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(true).isCompleted(false).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> service.markPlannedFinished(dayPartId, UUID.randomUUID(), 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Призначення вже відмінене");
        verify(partRepository, never()).save(any());
    }

    @Test
    void markPlannedFinished_throws_whenNotFound() {
        when(partRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markPlannedFinished(UUID.randomUUID(), UUID.randomUUID(), 7L))
                .isInstanceOf(NotFoundException.class);
    }

    // --- restoreToPlanned («Повернути у Заплановано») ---

    @Test
    void restoreToPlanned_clearsCancelFlagKeepsDoseAndAudits() {
        UUID dayPartId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Long userId = 7L;
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(true).isCompleted(false).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));
        when(partRepository.save(any())).thenReturn(part);

        PrescriptionDayPart result = service.restoreToPlanned(dayPartId, doctorId, userId);

        assertThat(result.getIsPlanned()).isTrue();
        assertThat(result.getIsPlannedFinished()).isFalse();
        assertThat(result.getIsCompleted()).isFalse();
        assertThat(result.getIsCompletedFinished()).isFalse();
        assertThat(result.getDose()).isEqualTo("50mg");
        verify(auditService).logAction("PrescriptionDayPart", dayPartId, "REPLAN", userId);
    }

    @Test
    void restoreToPlanned_throws_whenNotCancelled() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(false).isCompleted(false).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> service.restoreToPlanned(dayPartId, UUID.randomUUID(), 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Призначення не у статусі «Відмінено», повернення неможливе");
        verify(partRepository, never()).save(any());
        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void restoreToPlanned_throws_whenCompleted() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(false).isCompleted(true).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> service.restoreToPlanned(dayPartId, UUID.randomUUID(), 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Виконане призначення не може бути повернене у заплановані");
        verify(partRepository, never()).save(any());
    }

    @Test
    void restoreToPlanned_throws_whenCompletedFinished() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(true).isCompleted(true).isCompletedFinished(true).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> service.restoreToPlanned(dayPartId, UUID.randomUUID(), 7L))
                .isInstanceOf(BusinessException.class);
        verify(partRepository, never()).save(any());
    }

    @Test
    void restoreToPlanned_throws_whenNotFound() {
        when(partRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restoreToPlanned(UUID.randomUUID(), UUID.randomUUID(), 7L))
                .isInstanceOf(NotFoundException.class);
    }

    // --- cancelAssignment («Відмінити це призначення») ---

    @Test
    void cancelAssignment_resetsPlannedCellToUnplannedAndAudits() {
        UUID dayPartId = UUID.randomUUID();
        Long userId = 7L;
        PrescriptionDayPart target = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(false).isCompleted(false).isCompletedFinished(false)
                .doctorName("doctor-uuid").nurseName(null).build();
        target.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(target));
        when(partRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionDayPart result = service.cancelAssignment(dayPartId, userId);

        verify(partRepository, times(1)).save(partCaptor.capture());
        PrescriptionDayPart saved = partCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(dayPartId);
        assertThat(saved.getIsPlanned()).isFalse();
        assertThat(saved.getIsPlannedFinished()).isFalse();
        assertThat(saved.getDose()).isNull();
        assertThat(saved.getIsCompleted()).isFalse();
        assertThat(saved.getIsCompletedFinished()).isFalse();
        assertThat(saved.getDoctorName()).isNull();
        assertThat(saved.getNurseName()).isNull();
        assertThat(result).isSameAs(saved);
        verify(auditService).logAction("PrescriptionDayPart", dayPartId, "CANCEL_ASSIGNMENT", userId);
    }

    @Test
    void cancelAssignment_resetsCancelledCellToUnplanned() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("evening").dose("25mg")
                .isPlanned(true).isPlannedFinished(true).isCompleted(false).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));
        when(partRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionDayPart result = service.cancelAssignment(dayPartId, 7L);

        assertThat(result.getIsPlanned()).isFalse();
        assertThat(result.getIsPlannedFinished()).isFalse();
        assertThat(result.getDose()).isNull();
    }

    @Test
    void cancelAssignment_isIdempotentOnUnplannedCell() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("night")
                .isPlanned(false).isPlannedFinished(false).isCompleted(false).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));
        when(partRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionDayPart result = service.cancelAssignment(dayPartId, 7L);

        assertThat(result.getIsPlanned()).isFalse();
        assertThat(result.getDose()).isNull();
        verify(partRepository).save(any());
    }

    @Test
    void cancelAssignment_throws_whenCompleted() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(false).isCompleted(true).isCompletedFinished(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> service.cancelAssignment(dayPartId, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Виконане призначення не може бути відмінене");
        verify(partRepository, never()).save(any());
        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void cancelAssignment_throws_whenCompletedFinished() {
        UUID dayPartId = UUID.randomUUID();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg")
                .isPlanned(true).isPlannedFinished(false).isCompleted(true).isCompletedFinished(true).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> service.cancelAssignment(dayPartId, 7L))
                .isInstanceOf(BusinessException.class);
        verify(partRepository, never()).save(any());
    }

    @Test
    void cancelAssignment_throws_whenNotFound() {
        when(partRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelAssignment(UUID.randomUUID(), 7L))
                .isInstanceOf(NotFoundException.class);
    }

    // --- getDays ---

    @Test
    void getDays_returnsDaysForItem() {
        PrescriptionItemDay day = PrescriptionItemDay.builder()
                .item(testItem).dayDate(LocalDate.now()).build();
        day.setId(UUID.randomUUID());
        when(dayRepository.findByItemIdAndDeletedFalseOrderByDayDateAsc(itemId)).thenReturn(List.of(day));

        var result = service.getDays(itemId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDayDate()).isEqualTo(LocalDate.now());
    }

    // --- helpers ---

    private PrescriptionItemDay dayWithParts(LocalDate date, PrescriptionDayPart... parts) {
        PrescriptionItemDay day = PrescriptionItemDay.builder()
                .item(testItem)
                .dayDate(date)
                .build();
        day.setId(UUID.randomUUID());
        for (PrescriptionDayPart part : parts) {
            part.setDay(day);
        }
        day.getDayParts().addAll(List.of(parts));
        return day;
    }

    private PrescriptionItemDay dayWithDate(LocalDate date) {
        PrescriptionItemDay day = PrescriptionItemDay.builder()
                .item(testItem)
                .dayDate(date)
                .build();
        day.setId(UUID.randomUUID());
        return day;
    }

    private PrescriptionDayPart buildPart(String period, boolean isCompleted, boolean isCompletedFinished) {
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period(period)
                .isPlanned(false)
                .isPlannedFinished(false)
                .isCompleted(isCompleted)
                .isCompletedFinished(isCompletedFinished)
                .build();
        part.setId(UUID.randomUUID());
        return part;
    }
}
