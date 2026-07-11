package com.superhumans.repository;

import com.superhumans.entity.DayStatus;
import com.superhumans.entity.IcuDay;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IcuDayRepository extends JpaRepository<IcuDay, Long> {
    List<IcuDay> findByIcuCardIdOrderByDayNumberAsc(Long icuCardId);
    Optional<IcuDay> findByIcuCardIdAndDate(Long icuCardId, LocalDate date);
    List<IcuDay> findByStatusAndDateBefore(DayStatus status, LocalDate date);
    List<IcuDay> findByStatusAndDoctorIdAndDate(DayStatus status, Long doctorId, LocalDate date);
}
