package com.superhumans.repository;

import com.superhumans.entity.CareMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CareMeasureRepository extends JpaRepository<CareMeasure, Long> {
    List<CareMeasure> findByIcuDayIdOrderByHourAsc(Long icuDayId);
}
