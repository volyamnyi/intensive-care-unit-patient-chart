package com.superhumans.prosthesismanufacturing.notification;

import java.util.UUID;

/**
 * Domain event published when a brak is confirmed, i.e. after
 * {@code BrakService.createBrakAndBranch} persists the {@code BrakEvent}.
 *
 * <p>The confirmation itself happens inside the brak transaction; delivery
 * to listeners is deferred until after commit (see
 * {@code BrakNotificationListener}), so an email can never describe
 * a rolled-back brak.
 *
 * <p>Note: the confirming user is carried explicitly as {@code confirmedByUserId}
 * because {@code BrakEvent.createdBy} defaults to {@code 0} (it is never set
 * by the brak flow) and must not be used as the author identity.
 */
public record BrakConfirmedEvent(
        UUID brakEventId,
        UUID instanceId,
        UUID stageId,
        Long confirmedByUserId
) {
}
