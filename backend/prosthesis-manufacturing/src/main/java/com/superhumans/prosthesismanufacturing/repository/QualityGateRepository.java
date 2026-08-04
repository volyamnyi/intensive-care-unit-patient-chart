package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.QualityGate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualityGateRepository extends JpaRepository<QualityGate, UUID> {
    Optional<QualityGate> findByStageId(UUID stageId);
}
