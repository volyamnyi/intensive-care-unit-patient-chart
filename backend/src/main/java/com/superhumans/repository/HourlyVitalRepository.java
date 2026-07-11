package com.superhumans.repository;

import com.superhumans.entity.HourlyVital;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HourlyVitalRepository extends JpaRepository<HourlyVital, Long> {
    List<HourlyVital> findByIcuDayIdOrderByHourAsc(Long icuDayId);
    Optional<HourlyVital> findByIcuDayIdAndHour(Long icuDayId, Integer hour);
}
