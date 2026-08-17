package com.superhumans.icu.repository;

import com.superhumans.icu.entity.VentilationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VentilationSettingsRepository extends JpaRepository<VentilationSettings, UUID> {
    List<VentilationSettings> findByClinicalDayIdOrderByRecordHourAsc(UUID clinicalDayId);
    Optional<VentilationSettings> findByClinicalDayIdAndRecordHour(UUID clinicalDayId, Integer recordHour);
}
