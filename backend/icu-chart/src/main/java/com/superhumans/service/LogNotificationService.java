package com.superhumans.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * Stub notification service — logs events instead of sending real messages.
 * Replaced by TelegramNotificationService when telegram is enabled.
 */
@Slf4j
@Service
@ConditionalOnMissingBean(name = "telegramNotificationService")
public class LogNotificationService implements NotificationService {

    @Override
    public void notifyPrescriptionCreated(UUID listId, String patientName) {
        log.info("[NOTIFICATION] Prescription list created: id={}, patient={}", listId, patientName);
    }

    @Override
    public void notifyPrescriptionUpdated(UUID listId, String patientName) {
        log.info("[NOTIFICATION] Prescription list updated: id={}, patient={}", listId, patientName);
    }

    @Override
    public void notifyDoseExecuted(UUID listId, String medicineName, String nurseName) {
        log.info("[NOTIFICATION] Dose executed: listId={}, medicine={}, nurse={}", listId, medicineName, nurseName);
    }
}
