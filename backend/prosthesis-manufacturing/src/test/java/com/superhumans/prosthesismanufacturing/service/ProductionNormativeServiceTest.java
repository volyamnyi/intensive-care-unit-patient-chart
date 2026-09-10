package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.entity.core.SystemSettings;
import com.superhumans.exception.BadRequestException;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.repository.core.SystemSettingsRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionNormativeServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 12, 0, 0);

    @Mock SystemSettingsRepository settingsRepository;
    @Mock AuditService auditService;

    ProductionNormativeService service;

    @BeforeEach
    void setUp() {
        service = new ProductionNormativeService(settingsRepository, auditService);
    }

    @Test
    void get_absentRows_returnsDefaults() {
        when(settingsRepository.findByKey(any())).thenReturn(Optional.empty());

        ProductionNormativeService.Normative normative = service.get();

        assertThat(normative.overdueMultiplier()).isEqualTo(1.5);
        assertThat(normative.staleDays()).isEqualTo(7);
    }

    @Test
    void get_corruptRows_returnsDefaults() {
        when(settingsRepository.findByKey(any())).thenReturn(Optional.of(
                SystemSettings.builder().key("k").value("not-a-number").build()));

        assertThat(service.get().overdueMultiplier()).isEqualTo(1.5);
        assertThat(service.get().staleDays()).isEqualTo(7);
    }

    @Test
    void get_storedRows_areReturned() {
        when(settingsRepository.findByKey(ProductionNormativeService.OVERDUE_MULTIPLIER_KEY))
                .thenReturn(Optional.of(SystemSettings.builder()
                        .key(ProductionNormativeService.OVERDUE_MULTIPLIER_KEY)
                        .value("2.0").build()));
        when(settingsRepository.findByKey(ProductionNormativeService.STALE_DAYS_KEY))
                .thenReturn(Optional.of(SystemSettings.builder()
                        .key(ProductionNormativeService.STALE_DAYS_KEY).value("3").build()));

        ProductionNormativeService.Normative normative = service.get();

        assertThat(normative.overdueMultiplier()).isEqualTo(2.0);
        assertThat(normative.staleDays()).isEqualTo(3);
    }

    @Test
    void update_persistsAndAudits() {
        when(settingsRepository.findByKey(any())).thenReturn(Optional.empty());
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductionNormativeService.Normative normative = service.update(2.0, 3, 9L);

        assertThat(normative.overdueMultiplier()).isEqualTo(2.0);
        assertThat(normative.staleDays()).isEqualTo(3);
        ArgumentCaptor<SystemSettings> saved = ArgumentCaptor.forClass(SystemSettings.class);
        verify(settingsRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(SystemSettings::getKey)
                .containsExactlyInAnyOrder(
                        ProductionNormativeService.OVERDUE_MULTIPLIER_KEY,
                        ProductionNormativeService.STALE_DAYS_KEY);
        verify(auditService).logAction("SystemSettings", null, "PRODUCTION_NORMATIVE_UPDATE", 9L);
    }

    @Test
    void update_nullKeepsCurrent() {
        when(settingsRepository.findByKey(ProductionNormativeService.OVERDUE_MULTIPLIER_KEY))
                .thenReturn(Optional.of(SystemSettings.builder()
                        .key(ProductionNormativeService.OVERDUE_MULTIPLIER_KEY)
                        .value("2.5").build()));
        when(settingsRepository.findByKey(ProductionNormativeService.STALE_DAYS_KEY))
                .thenReturn(Optional.empty());
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProductionNormativeService.Normative normative = service.update(null, null, 9L);

        assertThat(normative.overdueMultiplier()).isEqualTo(2.5);
        assertThat(normative.staleDays()).isEqualTo(7);
    }

    @Test
    void update_rejectsOutOfRange() {
        assertThatThrownBy(() -> service.update(0.5, 7, 9L))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.update(5.5, 7, 9L))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.update(1.5, 0, 9L))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.update(1.5, 31, 9L))
                .isInstanceOf(BadRequestException.class);
        verify(settingsRepository, never()).save(any());
        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void isOverdue_matrix() {
        ProductionNormativeService.Normative normative =
                new ProductionNormativeService.Normative(1.5, 7);

        assertThat(ProductionNormativeService.isOverdue(5401L, 3600L, normative)).isTrue();
        assertThat(ProductionNormativeService.isOverdue(5400L, 3600L, normative)).isFalse();
        assertThat(ProductionNormativeService.isOverdue(999999L, null, normative)).isFalse();
        assertThat(ProductionNormativeService.isOverdue(null, 3600L, normative)).isFalse();
        assertThat(ProductionNormativeService.isOverdue(100L, 0L, normative)).isFalse();
    }

    @Test
    void isStale_matrix() {
        ProductionNormativeService.Normative normative =
                new ProductionNormativeService.Normative(1.5, 7);

        assertThat(ProductionNormativeService.isStale(
                FlowInstanceStatus.IN_PROGRESS, NOW.minusDays(7), NOW, normative)).isTrue();
        assertThat(ProductionNormativeService.isStale(
                FlowInstanceStatus.PAUSED, NOW.minusDays(30), NOW, normative)).isTrue();
        assertThat(ProductionNormativeService.isStale(
                FlowInstanceStatus.IN_PROGRESS, NOW.minusDays(6), NOW, normative)).isFalse();
        assertThat(ProductionNormativeService.isStale(
                FlowInstanceStatus.COMPLETED, NOW.minusDays(30), NOW, normative)).isFalse();
        assertThat(ProductionNormativeService.isStale(
                FlowInstanceStatus.FAILED, NOW.minusDays(30), NOW, normative)).isFalse();
        assertThat(ProductionNormativeService.isStale(
                FlowInstanceStatus.IN_PROGRESS, null, NOW, normative)).isFalse();
        assertThat(ProductionNormativeService.isStale(null, NOW.minusDays(30), NOW, normative))
                .isFalse();
    }
}
