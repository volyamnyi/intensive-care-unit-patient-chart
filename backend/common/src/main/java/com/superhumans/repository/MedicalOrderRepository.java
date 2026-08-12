package com.superhumans.repository;

import com.superhumans.entity.MedicalOrder;
import com.superhumans.entity.MedicalOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, UUID> {
    List<MedicalOrder> findByClinicalDayIdOrderByStartTimeAsc(UUID clinicalDayId);
    List<MedicalOrder> findByClinicalDayIdAndStatus(UUID clinicalDayId, MedicalOrderStatus status);
    List<MedicalOrder> findByClinicalDayIdAndCategory(UUID clinicalDayId, String category);
    long countByClinicalDayIdAndStatus(UUID clinicalDayId, MedicalOrderStatus status);
}
