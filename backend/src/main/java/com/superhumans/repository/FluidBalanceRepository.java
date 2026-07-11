package com.superhumans.repository;

import com.superhumans.entity.FluidBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FluidBalanceRepository extends JpaRepository<FluidBalance, Long> {
    Optional<FluidBalance> findByIcuDayId(Long icuDayId);
}
