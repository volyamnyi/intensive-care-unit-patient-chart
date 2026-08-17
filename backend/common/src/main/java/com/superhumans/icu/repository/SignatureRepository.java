package com.superhumans.icu.repository;

import com.superhumans.icu.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SignatureRepository extends JpaRepository<Signature, UUID> {
    List<Signature> findByClinicalDayId(UUID clinicalDayId);
    Optional<Signature> findByClinicalDayIdAndRole(UUID clinicalDayId, String role);
    List<Signature> findByClinicalDayIdAndStatus(UUID clinicalDayId, String status);
}
