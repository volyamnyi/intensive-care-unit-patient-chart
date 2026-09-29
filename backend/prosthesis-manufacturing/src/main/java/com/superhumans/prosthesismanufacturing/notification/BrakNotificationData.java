package com.superhumans.prosthesismanufacturing.notification;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fully resolved data for a confirmed training-prosthesis brak notification
 * (stage 6 of TP-LL-02, {@code POST /instances/{id}/brak}).
 *
 * <p>The composer ({@link BrakNotificationComposer}) never touches the database —
 * every value arrives here already resolved by the notification service.
 * Optional values may be {@code null}; the composer renders them as {@code «—»}.
 *
 * <p>Privacy: this record carries the minimum needed for identification —
 * the MIS patient id and order identifiers only. No patient names, contacts,
 * birth dates, diagnoses or clinical payloads belong here.
 *
 * <p>Epic #322 appends the order-wide escalation context: {@code brakCount}
 * is the chain count that triggered the email ({@code 1} for a per-brak
 * {@code SINGLE} notification), and {@code brakHistory} lists the chain
 * (empty for {@code SINGLE}).
 */
public record BrakNotificationData(
        String patientId,
        String orderNumber,
        UUID orderId,
        UUID originalInstanceId,
        UUID newInstanceId,
        String templateName,
        String stageLabel,
        String stepLabel,
        UUID stageId,
        UUID stepId,
        LocalDateTime confirmedAt,
        String confirmerName,
        String confirmerLogin,
        boolean softTissueMisalignment,
        boolean painDiscomfort,
        String note,
        String returnStageName,
        String frontendUrl,
        long brakCount,
        java.util.List<BrakHistoryEntry> brakHistory
) {
}
