package com.superhumans.prosthesismanufacturing.notification;

import java.time.LocalDateTime;

/**
 * One entry of the order-wide brak history (epic #322).
 *
 * <p>Carries the minimum needed to identify a past brak in the escalation
 * email: stage/step labels, confirmation time, confirmer display name and
 * the return stage. No patient data belongs here.
 */
public record BrakHistoryEntry(
        String stageLabel,
        String stepLabel,
        LocalDateTime confirmedAt,
        String confirmerName,
        String returnStageName
) {
}
