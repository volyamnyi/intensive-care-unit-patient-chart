package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.exception.NotFoundException;
import com.superhumans.repository.*;
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
        when(itemRepository.findByListIdOrderBySortOrderAsc(listId)).thenReturn(List.of(testItem));

        var result = service.getByList(listId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMedicineName()).isEqualTo("Dopamine");
    }

    @Test
    void getByList_returnsEmpty() {
        when(itemRepository.findByListIdOrderBySortOrderAsc(listId)).thenReturn(List.of());

        assertThat(service.getByList(listId)).isEmpty();
    }

    // --- addItem ---

    @Test
    void addItem_createsItemWith21DaysAnd4PartsEach() {
        when(listRepository.findById(listId)).thenReturn(Optional.of(testList));
        when(itemRepository.findByListId(listId)).thenReturn(List.of());
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
        when(itemRepository.findByListId(listId)).thenReturn(List.of(existing));
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
        when(itemRepository.findByListId(listId)).thenReturn(List.of());
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
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").isPlanned(false).isCompleted(false).build();
        part.setId(dayPartId);

        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(part));
        when(partRepository.save(any())).thenReturn(part);

        PrescriptionDayPart result = service.planDose(dayPartId, "50mg", doctorId);

        verify(partRepository).save(partCaptor.capture());
        assertThat(partCaptor.getValue().getDose()).isEqualTo("50mg");
        assertThat(partCaptor.getValue().getIsPlanned()).isTrue();
        assertThat(partCaptor.getValue().getDoctorName()).isEqualTo(doctorId.toString());
    }

    @Test
    void planDose_throws_whenNotFound() {
        when(partRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.planDose(UUID.randomUUID(), "10mg", UUID.randomUUID()))
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

    // --- getDays ---

    @Test
    void getDays_returnsDaysForItem() {
        PrescriptionItemDay day = PrescriptionItemDay.builder()
                .item(testItem).dayDate(LocalDate.now()).build();
        day.setId(UUID.randomUUID());
        when(dayRepository.findByItemIdOrderByDayDateAsc(itemId)).thenReturn(List.of(day));

        var result = service.getDays(itemId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDayDate()).isEqualTo(LocalDate.now());
    }
}
