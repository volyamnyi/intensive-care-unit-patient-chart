package com.superhumans.repository;

import com.superhumans.entity.FluidIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FluidIntakeRepository extends JpaRepository<FluidIntake, Long> {
    List<FluidIntake> findByIcuDayId(Long icuDayId);
    List<FluidIntake> findByIcuDayIdAndHour(Long icuDayId, Integer hour);
}
