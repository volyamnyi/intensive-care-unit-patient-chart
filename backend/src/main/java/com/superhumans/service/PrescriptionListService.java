package com.superhumans.service;

import com.superhumans.entity.PrescriptionList;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.repository.PrescriptionListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionListService {

    private final PrescriptionListRepository listRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<PrescriptionList> getByPatient(Long patientId) {
        return listRepository.findByPatientIdAndDeletedFalse(patientId);
    }

    @Transactional(readOnly = true)
    public PrescriptionList getById(UUID id) {
        return listRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Prescription list not found: " + id));
    }

    @Transactional
    public PrescriptionList create(Long patientId, Long userId) {
        PrescriptionList list = PrescriptionList.builder()
                .patientId(patientId)
                .documentName("Листок лікарських призначень")
                .status("Saved")
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        list = listRepository.save(list);
        log.info("Prescription list created: id={}, patientId={}", list.getId(), patientId);
        return list;
    }

    @Transactional
    public PrescriptionList update(UUID id, String documentName, Long userId) {
        PrescriptionList list = getById(id);
        acquireLock(list, userId);
        if (documentName != null) {
            list.setDocumentName(documentName);
        }
        list.setUpdatedBy(userId);
        list = listRepository.save(list);
        return list;
    }

    @Transactional
    public void acquireLock(PrescriptionList list, Long userId) {
        if (list.isFinished()) {
            throw new DocumentLockedException("Document is closed and cannot be edited");
        }
        if (list.isEditing() && !list.getEditingUserId().equals(userId)) {
            throw new DocumentLockedException("Document is being edited by another user");
        }
        list.setEditingUserId(userId);
        list.setEditingStartedAt(LocalDateTime.now());
        list.setStatus(userId.toString());
    }

    @Transactional
    public void releaseLock(UUID id, Long userId) {
        PrescriptionList list = getById(id);
        if (userId.equals(list.getEditingUserId())) {
            list.setEditingUserId(null);
            list.setEditingStartedAt(null);
            list.setStatus("Saved");
            list.setUpdatedBy(userId);
            listRepository.save(list);
        }
    }

    @Transactional
    public void close(UUID id, Long userId) {
        PrescriptionList list = getById(id);
        list.setStatus("Finished");
        list.setEditingUserId(null);
        list.setEditingStartedAt(null);
        list.setUpdatedBy(userId);
        listRepository.save(list);
        log.info("Prescription list closed: id={}", id);
    }

    @Transactional
    public void delete(UUID id) {
        PrescriptionList list = getById(id);
        list.setDeleted(true);
        listRepository.save(list);
        log.info("Prescription list deleted (soft): id={}", id);
    }
}
