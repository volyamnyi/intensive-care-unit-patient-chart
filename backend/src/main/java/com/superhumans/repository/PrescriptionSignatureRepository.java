package com.superhumans.repository;

import com.superhumans.entity.PrescriptionSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionSignatureRepository extends JpaRepository<PrescriptionSignature, UUID> {
    List<PrescriptionSignature> findByItemId(UUID itemId);
    List<PrescriptionSignature> findByUserId(UUID userId);
}
