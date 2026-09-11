package com.superhumans.medicationsheet.pdf;

/**
 * Single backend rule for rendering a {@code prescription_day_parts} row
 * on Form №003-4/о, in priority order (top wins).
 *
 * <ol>
 *   <li>{@code COMPLETED_FINISHED} — execution finished ({@code is_completed_finished})</li>
 *   <li>{@code COMPLETED} — actually executed ({@code is_completed} or an execution row)</li>
 *   <li>{@code CANCELLED} — planning cancelled ({@code is_planned + is_planned_finished}, dose retained)</li>
 *   <li>{@code PLANNED} — actively planned ({@code is_planned})</li>
 *   <li>{@code EMPTY} — no relevant data (also the state after {@code cancelAssignment})</li>
 * </ol>
 */
public enum PeriodPrintState {
    EMPTY,
    PLANNED,
    CANCELLED,
    COMPLETED,
    COMPLETED_FINISHED
}
