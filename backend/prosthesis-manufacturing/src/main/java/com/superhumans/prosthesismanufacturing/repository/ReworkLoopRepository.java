package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.ReworkLoop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReworkLoopRepository extends JpaRepository<ReworkLoop, UUID> {
    List<ReworkLoop> findByGateId(UUID gateId);
}
