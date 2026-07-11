package com.superhumans.repository;

import com.superhumans.entity.FluidOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FluidOutputRepository extends JpaRepository<FluidOutput, Long> {
    List<FluidOutput> findByIcuDayId(Long icuDayId);
    List<FluidOutput> findByIcuDayIdAndHour(Long icuDayId, Integer hour);
}
