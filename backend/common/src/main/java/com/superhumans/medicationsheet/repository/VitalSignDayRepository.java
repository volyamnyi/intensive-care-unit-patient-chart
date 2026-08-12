package com.superhumans.medicationsheet.repository;


import com.superhumans.medicationsheet.entity.VitalSignDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface VitalSignDayRepository extends JpaRepository<VitalSignDay, UUID> {
    List<VitalSignDay> findByVitalListIdOrderByDayDateAsc(UUID vitalListId);
}
