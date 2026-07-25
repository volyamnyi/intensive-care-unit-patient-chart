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
    public PrescriptionList create(Long patientId) {
        PrescriptionList list = PrescriptionList.builder()
                .patientId(patientId)
                .documentName("\u041B\u0438\u0441\u0442\u043E\u043A \u043B\u0456\u043A\u0430\u0440\u0441\u044C\u043A\u0438\u0445 \u043F\u0440\u0438\u0437\u043D\u0430\u0447\u0435\u043D\u044C")
                .status("Saved")
                .build();
        list.setCreatedBy(0L);
        list.setUpdatedBy(0L);
        list = listRepository.save(list);
        log.info("Prescription list created: id={}, patientId={}", list.getId(), patientId);
        return list;
    }

    @Transactional
    public PrescriptionList update(UUID id, String documentName) {
        PrescriptionList list = getById(id);
        if (documentName != null) {
            list.setDocumentName(documentName);
        }
        list.setUpdatedBy(0L);
        return listRepository.save(list);
    }

    @Transactional
    public void acquireLock(PrescriptionList list, UUID userId) {
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
    public void releaseLock(UUID id, UUID userId) {
        PrescriptionList list = getById(id);
        if (userId.equals(list.getEditingUserId())) {
            list.setEditingUserId(null);
            list.setEditingStartedAt(null);
            list.setStatus("Saved");
            list.setUpdatedBy(0L);
            listRepository.save(list);
        }
    }

    @Transactional
    public void close(UUID id) {
        PrescriptionList list = getById(id);
        list.setStatus("Finished");
        list.setEditingUserId(null);
        list.setEditingStartedAt(null);
        list.setUpdatedBy(0L);
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
