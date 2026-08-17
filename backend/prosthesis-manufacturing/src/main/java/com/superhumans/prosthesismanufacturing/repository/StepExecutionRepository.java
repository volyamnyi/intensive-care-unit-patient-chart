package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StepExecutionRepository extends JpaRepository<StepExecution, UUID> {
    List<StepExecution> findByInstanceId(UUID instanceId);
    List<StepExecution> findByInstanceIdAndStepId(UUID instanceId, UUID stepId);
    Optional<StepExecution> findByInstanceIdAndStepIdAndAttemptNumber(UUID instanceId, UUID stepId, Integer attemptNumber);
}
