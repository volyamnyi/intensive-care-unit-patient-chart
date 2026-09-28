package com.superhumans.prosthesismanufacturing.entity;

/**
 * Lifecycle of a queued brak email (issue #320, v2 guaranteed delivery).
 *
 * <p>{@code PENDING} rows are picked up by the eager after-commit listener and,
 * if still undelivered, by the scheduled sweep. {@code FAILED} rows are retried
 * until {@code attempts} reaches the configured maximum, then go {@code DEAD}.
 * {@code SENT} and {@code SKIPPED} are terminal successes; {@code DEAD} needs
 * an operator (fix SMTP, then reset the row to {@code PENDING}).
 */
public enum BrakNotificationStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED,
    DEAD
}
