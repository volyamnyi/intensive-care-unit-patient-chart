package com.superhumans.repository;

import com.superhumans.entity.PrescriptionItemDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionItemDayRepository extends JpaRepository<PrescriptionItemDay, UUID> {
    List<PrescriptionItemDay> findByItemId(UUID itemId);
    List<PrescriptionItemDay> findByItemIdOrderByDayDateAsc(UUID itemId);
}
