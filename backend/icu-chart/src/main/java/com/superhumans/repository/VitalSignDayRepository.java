package com.superhumans.repository;

import com.superhumans.entity.VitalSignDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface VitalSignDayRepository extends JpaRepository<VitalSignDay, UUID> {
    List<VitalSignDay> findByVitalListIdOrderByDayDateAsc(UUID vitalListId);
}
