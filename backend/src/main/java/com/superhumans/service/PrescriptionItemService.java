package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.exception.NotFoundException;
import com.superhumans.repository.*;
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
    private final DrugInteractionService drugInteractionService;

    @Transactional(readOnly = true)
    public List<PrescriptionItem> getByList(UUID listId) {
        return itemRepository.findByListIdOrderBySortOrderAsc(listId);
    }

    @Transactional
    public PrescriptionItem addItem(UUID listId, String medicineName, String method, String regime, Long userId) {
        PrescriptionList list = listRepository.findById(listId)
                .orElseThrow(() -> new NotFoundException("List not found: " + listId));

        int sortOrder = itemRepository.findByListId(listId).size();

        PrescriptionItem item = PrescriptionItem.builder()
                .list(list)
                .medicineName(medicineName)
                .medicineMethod(method)
                .regime(regime)
                .status("Active")
                .sortOrder(sortOrder)
                .build();
        item.setCreatedBy(userId);
        item.setUpdatedBy(userId);
        item = itemRepository.save(item);

        LocalDate startDate = LocalDate.now();
        for (int i = 0; i < 21; i++) {
            PrescriptionItemDay day = PrescriptionItemDay.builder()
                    .item(item)
                    .dayDate(startDate.plusDays(i))
                    .build();
            day.setCreatedBy(userId);
            day.setUpdatedBy(userId);
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
                part.setCreatedBy(userId);
                part.setUpdatedBy(userId);
                partRepository.save(part);
            }
        }
        log.info("Prescription item added: id={}, medicine={}, 21 days created", item.getId(), medicineName);
        return item;
    }

    @Transactional
    public void removeItem(UUID itemId, Long userId) {
        PrescriptionItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
        item.setDeleted(true);
        item.setUpdatedBy(userId);
        itemRepository.save(item);
    }

    @Transactional
    public PrescriptionDayPart getDayPart(UUID dayPartId) {
        return partRepository.findById(dayPartId)
                .orElseThrow(() -> new NotFoundException("Day part not found: " + dayPartId));
    }

    @Transactional
    public PrescriptionDayPart planDose(UUID dayPartId, String dose, Long userId, String role) {
        PrescriptionDayPart part = getDayPart(dayPartId);
        part.setDose(dose);
        part.setIsPlanned(true);
        part.setDoctorName(userId.toString());
        part.setUpdatedBy(userId);
        return partRepository.save(part);
    }

    @Transactional
    public PrescriptionDayPart markCompleted(UUID dayPartId, Long nurseId) {
        PrescriptionDayPart part = getDayPart(dayPartId);
        part.setIsCompleted(true);
        part.setNurseName(nurseId.toString());
        part.setUpdatedBy(nurseId);
        return partRepository.save(part);
    }

    @Transactional
    public PrescriptionDayPart markCompletedFinished(UUID dayPartId, Long userId) {
        PrescriptionDayPart part = getDayPart(dayPartId);
        part.setIsCompletedFinished(true);
        part.setUpdatedBy(userId);
        return partRepository.save(part);
    }

    @Transactional
    public List<PrescriptionItemDay> getDays(UUID itemId) {
        return dayRepository.findByItemIdOrderByDayDateAsc(itemId);
    }

    @Transactional
    public List<PrescriptionDayPart> getDayParts(UUID dayId) {
        return partRepository.findByDayId(dayId);
    }
}
