package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StepExecutionRepository extends JpaRepository<StepExecution, UUID> {
    List<StepExecution> findByInstanceId(UUID instanceId);
    List<StepExecution> findByInstanceIdAndStepId(UUID instanceId, UUID stepId);
    Optional<StepExecution> findByInstanceIdAndStepIdAndAttemptNumber(UUID instanceId, UUID stepId, Integer attemptNumber);

    /**
     * Batch active-time sums per instance for the production read-model.
     * Returns {@code [instanceId, sum]} rows; instances without executions are absent.
     */
    @Query("select e.instance.id, coalesce(sum(e.activeSeconds), 0) from StepExecution e "
            + "where e.instance.id in :ids group by e.instance.id")
    List<Object[]> sumActiveSecondsByInstanceIds(@Param("ids") Collection<UUID> ids);
}
