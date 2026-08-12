package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.GateDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface GateDecisionRepository extends JpaRepository<GateDecision, UUID> {
    List<GateDecision> findByInstanceId(UUID instanceId);
    List<GateDecision> findByInstanceIdAndGateId(UUID instanceId, UUID gateId);
}
