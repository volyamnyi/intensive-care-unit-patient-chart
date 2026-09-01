package com.superhumans.prosthesismanufacturing.repository;

import com.superhumans.prosthesismanufacturing.entity.BrakEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BrakEventRepository extends JpaRepository<BrakEvent, UUID> {
    List<BrakEvent> findByInstanceId(UUID instanceId);
    List<BrakEvent> findByNewInstanceId(UUID newInstanceId);
}
