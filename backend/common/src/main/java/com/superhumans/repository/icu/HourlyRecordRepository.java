package com.superhumans.repository.icu;

import com.superhumans.entity.HourlyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HourlyRecordRepository extends JpaRepository<HourlyRecord, UUID> {
    List<HourlyRecord> findByClinicalDayIdOrderByRecordTimeAsc(UUID clinicalDayId);
    Optional<HourlyRecord> findByClinicalDayIdAndRecordTime(UUID clinicalDayId, LocalDateTime recordTime);
    Optional<HourlyRecord> findByClinicalDayIdAndRecordHour(UUID clinicalDayId, Integer recordHour);
    List<HourlyRecord> findByClinicalDayIdAndRecordTimeBetweenOrderByRecordTimeAsc(
            UUID clinicalDayId, LocalDateTime start, LocalDateTime end);
    void deleteByClinicalDayId(UUID clinicalDayId);
    long countByClinicalDayId(UUID clinicalDayId);
}
