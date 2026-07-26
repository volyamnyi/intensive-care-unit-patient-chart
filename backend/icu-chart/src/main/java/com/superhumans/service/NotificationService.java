package com.superhumans.service;

import java.util.UUID;

/**
 * Notification service for prescription-related events.
 * <p>
 * Current implementation: {@link LogNotificationService} — logs to SLF4J.
 * TODO: Replace with {@code TelegramNotificationService} (Issue #59) for real Telegram notifications.
 */
public interface NotificationService {

    void notifyPrescriptionCreated(UUID listId, String patientName);

    void notifyPrescriptionUpdated(UUID listId, String patientName);

    void notifyDoseExecuted(UUID listId, String medicineName, String nurseName);
}
