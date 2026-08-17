package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.FailureSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FailureSnapshotRepository extends JpaRepository<FailureSnapshot, UUID> {
    Optional<FailureSnapshot> findByInstanceId(UUID instanceId);
}
