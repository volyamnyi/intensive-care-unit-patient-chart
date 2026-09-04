package com.superhumans.medicationsheet.service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.exception.BusinessException;
import com.superhumans.exception.ErrorCode;
import com.superhumans.exception.NotFoundException;
import com.superhumans.service.AuditService;
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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrescriptionItemService {

    PrescriptionItemRepository itemRepository;
    PrescriptionItemDayRepository dayRepository;
    PrescriptionDayPartRepository partRepository;
    PrescriptionListRepository listRepository;
    AuditService auditService;

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
    public PrescriptionItemDay addDay(UUID itemId) {
        PrescriptionItem item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
        if (Boolean.TRUE.equals(item.getDeleted())) {
            throw new NotFoundException("Item not found: " + itemId);
        }

        List<PrescriptionItemDay> existingDays = dayRepository.findByItemIdAndDeletedFalseOrderByDayDateAsc(itemId);
        LocalDate nextDate = existingDays.stream()
                .map(PrescriptionItemDay::getDayDate)
                .max(java.util.Comparator.naturalOrder())
                .map(d -> d.plusDays(1))
                .orElse(LocalDate.now());

        PrescriptionItemDay day = createDay(item, nextDate);
        log.info("Prescription day added: itemId={}, dayDate={}", item.getId(), nextDate);
        return day;
    }

    @Transactional
    public void removeDay(UUID itemId, UUID dayId) {
        PrescriptionItemDay day = dayRepository.findById(dayId)
                .orElseThrow(() -> new NotFoundException("Day not found: " + dayId));
        if (Boolean.TRUE.equals(day.getDeleted())) {
            throw new NotFoundException("Day not found: " + dayId);
        }
        if (day.getItem() == null || !itemId.equals(day.getItem().getId())) {
            throw new NotFoundException("Day not found: " + dayId);
        }

        boolean hasExecuted = day.getDayParts() != null && day.getDayParts().stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.getIsCompleted()) || Boolean.TRUE.equals(p.getIsCompletedFinished()));
        if (hasExecuted) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "День містить виконані призначення, видалення неможливе");
        }

        day.setDeleted(true);
        day.setUpdatedBy(0L);
        dayRepository.save(day);
        log.info("Prescription day removed: itemId={}, dayId={}", itemId, dayId);
    }

    private PrescriptionItemDay createDay(PrescriptionItem item, LocalDate dayDate) {
        PrescriptionItemDay day = PrescriptionItemDay.builder()
                .item(item)
                .dayDate(dayDate)
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
        return day;
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
    public PrescriptionDayPart planDose(UUID dayPartId, String dose, UUID doctorId, Long userId) {
        PrescriptionDayPart part = getDayPart(dayPartId);
        part.setDose(dose);
        part.setIsPlanned(true);
        // Re-planning a cancelled dose («Відмінити препарат») returns it to Scheduled:
        // the cancel flag must be cleared, otherwise the cell keeps rendering «Відмінено».
        part.setIsPlannedFinished(false);
        part.setDoctorName(doctorId.toString());
        part.setUpdatedBy(userId);
        PrescriptionDayPart saved = partRepository.save(part);
        auditService.logAction("PrescriptionDayPart", saved.getId(), "PLAN", userId);
        return saved;
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
    public PrescriptionDayPart markPlannedFinished(UUID dayPartId, UUID doctorId, Long userId) {
        PrescriptionDayPart part = getDayPart(dayPartId);
        if (Boolean.TRUE.equals(part.getIsCompleted()) || Boolean.TRUE.equals(part.getIsCompletedFinished())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Виконане призначення не може бути відмінене");
        }
        if (!Boolean.TRUE.equals(part.getIsPlanned())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Незаплановане призначення не може бути відмінене");
        }
        if (Boolean.TRUE.equals(part.getIsPlannedFinished())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Призначення вже відмінене");
        }
        part.setIsPlannedFinished(true);
        part.setDoctorName(doctorId.toString());
        part.setUpdatedBy(userId);
        PrescriptionDayPart saved = partRepository.save(part);
        auditService.logAction("PrescriptionDayPart", saved.getId(), "CANCEL", userId);
        return saved;
    }

    @Transactional
    public PrescriptionDayPart restoreToPlanned(UUID dayPartId, UUID doctorId, Long userId) {
        PrescriptionDayPart part = getDayPart(dayPartId);
        if (Boolean.TRUE.equals(part.getIsCompleted()) || Boolean.TRUE.equals(part.getIsCompletedFinished())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Виконане призначення не може бути повернене у заплановані");
        }
        if (!Boolean.TRUE.equals(part.getIsPlannedFinished())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Призначення не у статусі «Відмінено», повернення неможливе");
        }
        part.setIsPlanned(true);
        part.setIsPlannedFinished(false);
        part.setDoctorName(doctorId.toString());
        part.setUpdatedBy(userId);
        PrescriptionDayPart saved = partRepository.save(part);
        auditService.logAction("PrescriptionDayPart", saved.getId(), "REPLAN", userId);
        return saved;
    }

    @Transactional
    public PrescriptionDayPart cancelAssignment(UUID dayPartId, Long userId) {
        PrescriptionDayPart part = getDayPart(dayPartId);
        if (Boolean.TRUE.equals(part.getIsCompleted()) || Boolean.TRUE.equals(part.getIsCompletedFinished())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Виконане призначення не може бути відмінене");
        }
        // «Відмінити це призначення»: reset exactly this period cell to the
        // unplanned (white) zero-state. Sibling cells are untouched — the caller
        // addresses them by their own dayPartId.
        part.setIsPlanned(false);
        part.setIsPlannedFinished(false);
        part.setDose(null);
        part.setIsCompleted(false);
        part.setIsCompletedFinished(false);
        part.setDoctorName(null);
        part.setNurseName(null);
        part.setUpdatedBy(userId);
        PrescriptionDayPart saved = partRepository.save(part);
        auditService.logAction("PrescriptionDayPart", saved.getId(), "CANCEL_ASSIGNMENT", userId);
        return saved;
    }

    public List<PrescriptionItemDay> getDays(UUID itemId) {
        return dayRepository.findByItemIdAndDeletedFalseOrderByDayDateAsc(itemId);
    }

    @Transactional(readOnly = true)
    public PrescriptionItem getListItem(UUID itemId) {
        PrescriptionItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
        if (Boolean.TRUE.equals(item.getDeleted())) {
            throw new NotFoundException("Item not found: " + itemId);
        }
        item.getList();
        return item;
    }
}
