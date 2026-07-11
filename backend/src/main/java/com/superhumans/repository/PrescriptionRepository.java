package com.superhumans.repository;

import com.superhumans.entity.Prescription;
import com.superhumans.entity.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByIcuCardIdOrderByCreatedAtAsc(Long icuCardId);
    List<Prescription> findByIcuCardIdAndStatus(Long icuCardId, PrescriptionStatus status);
}
