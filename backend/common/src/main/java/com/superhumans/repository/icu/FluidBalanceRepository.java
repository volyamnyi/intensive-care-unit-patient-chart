package com.superhumans.repository.icu;

import com.superhumans.entity.FluidBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FluidBalanceRepository extends JpaRepository<FluidBalance, UUID> {
    List<FluidBalance> findByClinicalDayIdOrderByHourAsc(UUID clinicalDayId);
    Optional<FluidBalance> findByClinicalDayIdAndHour(UUID clinicalDayId, Integer hour);
    void deleteByClinicalDayId(UUID clinicalDayId);

    @Query("SELECT COALESCE(SUM(fb.balance), 0) FROM FluidBalance fb WHERE fb.clinicalDay.id = :clinicalDayId")
    Double calculateDailyBalance(@Param("clinicalDayId") UUID clinicalDayId);
}
