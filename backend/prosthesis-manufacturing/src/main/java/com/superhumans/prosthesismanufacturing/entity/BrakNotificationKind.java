package com.superhumans.prosthesismanufacturing.entity;

/**
 * Kind of a queued brak email (epic #322).
 *
 * <p>{@code SINGLE} is the per-brak stage-6 notification from issue #320 (one
 * row per confirmed stage-6 brak). {@code THRESHOLD} is the order-wide
 * escalation: one row per triggering brak event whose order chain reached
 * brak count &gt;= 3 over both trigger steps (d0000017/e0000028 and
 * d0000020/e0000032, summed per orderId), delivered to the same
 * {@code PROSTHETICS_ADMINISTRATOR} recipients.
 */
public enum BrakNotificationKind {
    SINGLE,
    THRESHOLD
}
