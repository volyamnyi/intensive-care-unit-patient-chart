package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.entity.core.SystemSettings;
import com.superhumans.exception.BadRequestException;
import com.superhumans.prosthesismanufacturing.dto.ProductionNormativeDto;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.repository.core.SystemSettingsRepository;
import com.superhumans.service.AuditService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Editable normative thresholds for production attention flags
 * (manufacturing epic #271, issue #277).
 *
 * <p>Rules: an item is OVERDUE when {@code elapsed > expected * K};
 * STALE when an open item saw no activity for {@code staleDays}.
 * Values live in {@code SystemSettings} (no schema change); absent or
 * corrupt rows silently fall back to the defaults below.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductionNormativeService {

    public static final String OVERDUE_MULTIPLIER_KEY = "prosthetics.production.overdueMultiplier";
    public static final String STALE_DAYS_KEY = "prosthetics.production.staleDays";

    public static final double DEFAULT_OVERDUE_MULTIPLIER = 1.5;
    public static final int DEFAULT_STALE_DAYS = 7;

    public static final double MIN_OVERDUE_MULTIPLIER = 1.0;
    public static final double MAX_OVERDUE_MULTIPLIER = 5.0;
    public static final int MIN_STALE_DAYS = 1;
    public static final int MAX_STALE_DAYS = 30;

    /** Attention flag for elapsed time beyond the norm. */
    public static final String FLAG_OVERDUE = "OVERDUE";
    /** Attention flag for open items without recent activity. */
    public static final String FLAG_STALE = "STALE";

    /** Statuses that count as "open" for the STALE rule. */
    static final Set<FlowInstanceStatus> OPEN_STATUSES = EnumSet.of(
            FlowInstanceStatus.NEW, FlowInstanceStatus.IN_PROGRESS, FlowInstanceStatus.PAUSED,
            FlowInstanceStatus.BLOCKED_PATIENT, FlowInstanceStatus.BLOCKED_MATERIAL);

    SystemSettingsRepository settingsRepository;
    AuditService auditService;

    /** Effective thresholds (defaults for absent/corrupt rows). */
    public record Normative(double overdueMultiplier, int staleDays) {
    }

    @Transactional(readOnly = true)
    public Normative get() {
        return new Normative(readMultiplier(), readStaleDays());
    }

    /**
     * Updates thresholds (null keeps the current value).
     *
     * @throws BadRequestException when a provided value is out of range
     */
    @Transactional
    public Normative update(Double overdueMultiplier, Integer staleDays, Long userId) {
        double multiplier = overdueMultiplier == null ? readMultiplier() : overdueMultiplier;
        int days = staleDays == null ? readStaleDays() : staleDays;
        if (multiplier < MIN_OVERDUE_MULTIPLIER || multiplier > MAX_OVERDUE_MULTIPLIER) {
            throw new BadRequestException("Коефіцієнт прострочення має бути від "
                    + MIN_OVERDUE_MULTIPLIER + " до " + MAX_OVERDUE_MULTIPLIER);
        }
        if (days < MIN_STALE_DAYS || days > MAX_STALE_DAYS) {
            throw new BadRequestException("Поріг застою має бути від "
                    + MIN_STALE_DAYS + " до " + MAX_STALE_DAYS + " днів");
        }
        write(OVERDUE_MULTIPLIER_KEY, String.valueOf(multiplier),
                "Множник прострочення виробництва (elapsed > expected * K)");
        write(STALE_DAYS_KEY, String.valueOf(days),
                "Днів без активності для прапорця STALE");
        auditService.logAction("SystemSettings", null, "PRODUCTION_NORMATIVE_UPDATE", userId);
        return new Normative(multiplier, days);
    }

    /** Snapshot for API responses. */
    public static ProductionNormativeDto toDto(Normative normative) {
        return ProductionNormativeDto.builder()
                .overdueMultiplier(normative.overdueMultiplier())
                .staleDays(normative.staleDays())
                .build();
    }

    static boolean isOverdue(Long elapsedSeconds, Long expectedActiveSeconds, Normative normative) {
        if (elapsedSeconds == null || expectedActiveSeconds == null || expectedActiveSeconds <= 0) {
            return false;
        }
        return elapsedSeconds > expectedActiveSeconds * normative.overdueMultiplier();
    }

    static boolean isStale(FlowInstanceStatus status, LocalDateTime lastActivityAt,
            LocalDateTime now, Normative normative) {
        if (status == null || !OPEN_STATUSES.contains(status) || lastActivityAt == null) {
            return false;
        }
        return Duration.between(lastActivityAt, now).toDays() >= normative.staleDays();
    }

    private double readMultiplier() {
        return readDouble(OVERDUE_MULTIPLIER_KEY, DEFAULT_OVERDUE_MULTIPLIER);
    }

    private int readStaleDays() {
        double value = readDouble(STALE_DAYS_KEY, DEFAULT_STALE_DAYS);
        int days = (int) value;
        return days == value && days >= MIN_STALE_DAYS && days <= MAX_STALE_DAYS
                ? days : DEFAULT_STALE_DAYS;
    }

    private double readDouble(String key, double fallback) {
        try {
            return settingsRepository.findByKey(key)
                    .map(SystemSettings::getValue)
                    .map(String::trim)
                    .map(Double::parseDouble)
                    .filter(v -> !v.isNaN() && !v.isInfinite())
                    .orElse(fallback);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private void write(String key, String value, String description) {
        SystemSettings settings = settingsRepository.findByKey(key).orElseGet(() ->
                SystemSettings.builder().key(key).description(description).build());
        settings.setValue(value);
        if (settings.getDescription() == null) {
            settings.setDescription(description);
        }
        settingsRepository.save(settings);
    }
}
