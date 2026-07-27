package com.superhumans.medicationsheet.service;

import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemDayRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemRepository;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionItemService {

    private final PrescriptionItemRepository itemRepository;
    private final PrescriptionItemDayRepository dayRepository;
    private final PrescriptionDayPartRepository partRepository;
    private final PrescriptionListRepository listRepository;

    @Transactional(readOnly = true)
    public List<PrescriptionItem> getByList(UUID listId) {
        return itemRepository.findByListIdAndDeletedFalseOrderBySortOrderAsc(listId);
    }

    @Transactional
    public PrescriptionItem addItem(UUID listId, String medicineName, String method, String regime) {
        PrescriptionList list = listRepository.findById(listId)
                .orElseThrow(() -> new NotFoundException("List not found: " + listId));

        int sortOrder = itemRepository.findByListIdAndDeletedFalseOrderBySortOrderAsc(listId).size();

        PrescriptionItem item = PrescriptionItem.builder()
                .list(list)
                .medicineName(medicineName)
                .medicineMethod(method)
                .regime(regime)
                .status("Active")
                .sortOrder(sortOrder)
                .build();
        item.setCreatedBy(0L);
        item.setUpdatedBy(0L);
        item = itemRepository.save(item);

        LocalDate startDate = LocalDate.now();
        for (int i = 0; i < 21; i++) {
            PrescriptionItemDay day = PrescriptionItemDay.builder()
                    .item(item)
                    .dayDate(startDate.plusDays(i))
                    .build();
            day.setCreatedBy(0L);
            day.setUpdatedBy(0L);
            day = dayRepository.save(day);

            for (String period : List.of("morning", "day", "evening", "night")) {
                PrescriptionDayPart part = PrescriptionDayPart.builder()
                        .day(day)
                        .period(period)
                        .isPlanned(false)
                        .isPlannedFinished(false)
                        .isCompleted(false)
                        .isCompletedFinished(false)
                        .build();
                part.setCreatedBy(0L);
                part.setUpdatedBy(0L);
                partRepository.save(part);
            }
        }
        log.info("Prescription item added: id={}, medicine={}, 21 days created", item.getId(), medicineName);
        return item;
    }

    @Transactional
    public void removeItem(UUID itemId) {
        PrescriptionItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
        item.setDeleted(true);
        item.setUpdatedBy(0L);
        itemRepository.save(item);
    }

    @Transactional
    public PrescriptionDayPart getDayPart(UUID dayPartId) {
        return partRepository.findById(dayPartId)
                .orElseThrow(() -> new NotFoundException("Day part not found: " + dayPartId));
    }

    @Transactional
    public PrescriptionDayPart planDose(UUID dayPartId, String dose, UUID doctorId) {
        PrescriptionDayPart part = getDayPart(dayPartId);
        part.setDose(dose);
        part.setIsPlanned(true);
        part.setDoctorName(doctorId.toString());
        part.setUpdatedBy(0L);
        return partRepository.save(part);
    }

    @Transactional
    public PrescriptionDayPart markCompleted(UUID dayPartId, UUID nurseId) {
        PrescriptionDayPart part = getDayPart(dayPartId);
        part.setIsCompleted(true);
        part.setNurseName(nurseId.toString());
        part.setUpdatedBy(0L);
        return partRepository.save(part);
    }

    @Transactional
    public PrescriptionDayPart markCompletedFinished(UUID dayPartId) {
        PrescriptionDayPart part = getDayPart(dayPartId);
        part.setIsCompletedFinished(true);
        part.setUpdatedBy(0L);
        return partRepository.save(part);
    }

    @Transactional
    public PrescriptionDayPart markPlannedFinished(UUID dayPartId, UUID doctorId) {
        PrescriptionDayPart part = getDayPart(dayPartId);
        part.setIsPlannedFinished(true);
        part.setDoctorName(doctorId.toString());
        part.setUpdatedBy(0L);
        return partRepository.save(part);
    }

    public List<PrescriptionItemDay> getDays(UUID itemId) {
        return dayRepository.findByItemIdOrderByDayDateAsc(itemId);
    }
}
