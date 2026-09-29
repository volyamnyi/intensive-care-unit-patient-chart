package com.superhumans.prosthesismanufacturing.service;

/**
 * Threshold rule for the brak escalation (epic #322, issue #324).
 *
 * <p>Notifies the prosthetics administrator when the order-wide brak count
 * exceeds {@link #THRESHOLD}. The threshold is a fixed domain constant
 * (same style as the {@code STAGE_*} trigger constants in
 * {@link BrakService}), not a runtime setting: the business rule is
 * literally "more than 2 times".
 *
 * <p>Owner decision (#322): the notification repeats on <em>every</em> brak
 * with {@code count >= 3} (3rd, 4th, 5th, ...), so this is a pure
 * {@code count > THRESHOLD} predicate. Per-event idempotency (one
 * notification row per triggering brak) is enforced by
 * {@link BrakThresholdService} via the {@code UNIQUE(brak_event_id)}
 * constraint, not here.
 */
public final class BrakThresholdPolicy {

    /** Fixed business rule: notify when the order chain exceeds 2 braks. */
    public static final long THRESHOLD = 2L;

    /** Count that triggers the first escalation (THRESHOLD exceeded). */
    public static final long FIRST_TRIGGER_COUNT = THRESHOLD + 1;

    private BrakThresholdPolicy() {
    }

    /**
     * Returns {@code true} when the given order-chain brak count (already
     * including the just-saved event) must trigger an escalation.
     */
    public static boolean shouldNotify(long brakCount) {
        return brakCount > THRESHOLD;
    }
}
