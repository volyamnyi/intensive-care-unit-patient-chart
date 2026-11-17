package com.superhumans.medicationsheet.service;

import com.superhumans.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V1–V4 matrix from docs/Actual-Time-Design.md §5 (pure static — no Spring).
 */
class ActualTimeValidatorTest {

    private static final LocalDateTime NOW = LocalDateTime.parse("2026-01-15T12:00:00");
    private static final LocalDate MIN_DAY = LocalDate.parse("2026-01-10");
    private static final LocalDate CELL_DAY = LocalDate.parse("2026-01-15");

    // --- V1: parse ---

    @Test
    void parse_acceptsSeconds() {
        assertThat(ActualTimeValidator.parseActualTime("2026-01-15T10:30:07"))
                .isEqualTo(LocalDateTime.parse("2026-01-15T10:30:07"));
    }

    @Test
    void parse_acceptsMinutesOnly_andNormalizesZeroSeconds() {
        assertThat(ActualTimeValidator.parseActualTime("2026-01-15T10:30"))
                .isEqualTo(LocalDateTime.parse("2026-01-15T10:30:00"));
    }

    @Test
    void parse_nullOrBlank_meansNotSent() {
        assertThat(ActualTimeValidator.parseActualTime(null)).isNull();
        assertThat(ActualTimeValidator.parseActualTime(" ")).isNull();
    }

    @Test
    void parse_garbage_throwsIllegalArgument() {
        assertThatThrownBy(() -> ActualTimeValidator.parseActualTime("15/01/2026 10:30"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ActualTimeValidator.parseActualTime("2026-01-15T10:30:07Z"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ActualTimeValidator.parseActualTime("2026-13-01T10:30"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ActualTimeValidator.parseActualTime("2026-01-15"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- V2: not in the future (now + 5 min inclusive) ---

    @Test
    void v2_boundNowPlusFiveMinutesInclusive_isAccepted() {
        assertThatCode(() -> ActualTimeValidator.assertNotInFuture(NOW.plusMinutes(5), NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void v2_beyondTolerance_isRejected() {
        assertThatThrownBy(() -> ActualTimeValidator.assertNotInFuture(NOW.plusMinutes(5).plusSeconds(1), NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_FUTURE);
        assertThatThrownBy(() -> ActualTimeValidator.assertNotInFuture(NOW.plusHours(1), NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_FUTURE);
    }

    @Test
    void v2_past_isAccepted() {
        assertThatCode(() -> ActualTimeValidator.assertNotInFuture(NOW.minusHours(1), NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void v2_null_isSkipped() {
        assertThatCode(() -> ActualTimeValidator.assertNotInFuture(null, NOW))
                .doesNotThrowAnyException();
    }

    // --- V3: list-period window [minDayDate 00:00; cellDayDate + 1 23:59:59] ---

    @Test
    void v3_windowBoundsInclusive_areAccepted() {
        // lower bound: the first day of the list, 00:00
        assertThatCode(() -> ActualTimeValidator.assertWithinListPeriod(
                LocalDateTime.parse("2026-01-10T00:00:00"), MIN_DAY, CELL_DAY))
                .doesNotThrowAnyException();
        // upper bound: cellDayDate + 1 - a night dose of day D documented the next morning
        assertThatCode(() -> ActualTimeValidator.assertWithinListPeriod(
                LocalDateTime.parse("2026-01-16T23:59:59"), MIN_DAY, CELL_DAY))
                .doesNotThrowAnyException();
        assertThatCode(() -> ActualTimeValidator.assertWithinListPeriod(
                LocalDateTime.parse("2026-01-16T08:00:00"), MIN_DAY, CELL_DAY))
                .doesNotThrowAnyException();
    }

    @Test
    void v3_outsideWindow_isRejected() {
        assertThatThrownBy(() -> ActualTimeValidator.assertWithinListPeriod(
                LocalDateTime.parse("2026-01-09T23:59:59"), MIN_DAY, CELL_DAY))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_OUTSIDE_LIST);
        assertThatThrownBy(() -> ActualTimeValidator.assertWithinListPeriod(
                LocalDateTime.parse("2026-01-17T00:00:00"), MIN_DAY, CELL_DAY))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_OUTSIDE_LIST);
    }

    @Test
    void v3_missingContext_orNullValue_isSkipped() {
        assertThatCode(() -> ActualTimeValidator.assertWithinListPeriod(
                LocalDateTime.parse("2020-01-01T00:00:00"), null, CELL_DAY))
                .doesNotThrowAnyException();
        assertThatCode(() -> ActualTimeValidator.assertWithinListPeriod(
                LocalDateTime.parse("2020-01-01T00:00:00"), MIN_DAY, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> ActualTimeValidator.assertWithinListPeriod(null, MIN_DAY, CELL_DAY))
                .doesNotThrowAnyException();
    }

    // --- V4: dispensing not after administration ---

    @Test
    void v4_equalTimes_areAccepted() {
        LocalDateTime same = LocalDateTime.parse("2026-01-15T10:30:00");
        assertThatCode(() -> ActualTimeValidator.assertDispensedNotAfterAdministered(same, same))
                .doesNotThrowAnyException();
    }

    @Test
    void v4_dispensedAfterAdministered_isRejected() {
        assertThatThrownBy(() -> ActualTimeValidator.assertDispensedNotAfterAdministered(
                LocalDateTime.parse("2026-01-15T10:30:00"), LocalDateTime.parse("2026-01-15T10:30:01")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_DISPENSED_AFTER_ADMINISTERED);
    }

    @Test
    void v4_nullSide_isSkipped() {
        assertThatCode(() -> ActualTimeValidator.assertDispensedNotAfterAdministered(
                null, LocalDateTime.parse("2026-01-15T10:30:00")))
                .doesNotThrowAnyException();
        assertThatCode(() -> ActualTimeValidator.assertDispensedNotAfterAdministered(
                LocalDateTime.parse("2026-01-15T10:30:00"), null))
                .doesNotThrowAnyException();
    }
}
