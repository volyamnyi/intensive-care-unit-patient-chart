package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlowInstanceRepository extends JpaRepository<FlowInstance, UUID> {
    List<FlowInstance> findByAssignedUserId(Long assignedUserId);
    List<FlowInstance> findByAssignedUserIdAndStatus(Long assignedUserId, FlowInstanceStatus status);
    List<FlowInstance> findByStatus(FlowInstanceStatus status);
    Page<FlowInstance> findByAssignedUserId(Long assignedUserId, Pageable pageable);
    Page<FlowInstance> findByAssignedUserIdAndStatus(Long assignedUserId, FlowInstanceStatus status,
            Pageable pageable);
    Page<FlowInstance> findByStatus(FlowInstanceStatus status, Pageable pageable);
    List<FlowInstance> findByOrderId(UUID orderId);
    List<FlowInstance> findByParentInstanceId(UUID parentInstanceId);
    List<FlowInstance> findByOrderIdOrderByBranchSequence(UUID orderId);

    /**
     * Batch rework counts: number of branch instances per originating instance,
     * for the production read-model. Returns {@code [parentInstanceId, count]}
     * rows; instances without branches are absent.
     */
    @Query("select i.parentInstanceId, count(i) from FlowInstance i "
            + "where i.parentInstanceId in :ids group by i.parentInstanceId")
    List<Object[]> countChildrenByParentIds(@Param("ids") Collection<UUID> ids);

    // ------------------------------------------------------------------
    // Dashboard aggregates (#429 item 2)
    //
    // The KPI summary and the attention queue used to call findAll() and then
    // parse every template_snapshot JSONB. These projections keep the
    // dashboard query count flat regardless of history size.
    // ------------------------------------------------------------------

    /** {@code [status, count]} over the caller's scope; null assignee = team scope. */
    @Query("select i.status, count(i) from FlowInstance i "
            + "where (:assigneeId is null or i.assignedUserId = :assigneeId) "
            + "group by i.status")
    List<Object[]> countByStatusGrouped(@Param("assigneeId") Long assigneeId);

    /** Distinct instances carrying at least one brak event. */
    @Query("select count(distinct i.id) from FlowInstance i "
            + "join BrakEvent b on b.instanceId = i.id "
            + "where (:assigneeId is null or i.assignedUserId = :assigneeId)")
    long countInstancesWithBrak(@Param("assigneeId") Long assigneeId);

    /** Distinct instances that are the parent of at least one branch (rework). */
    @Query("select count(distinct i.id) from FlowInstance i "
            + "where i.parentInstanceId is not null "
            + "and (:assigneeId is null or i.assignedUserId = :assigneeId)")
    long countInstancesWithRework(@Param("assigneeId") Long assigneeId);

    /**
     * Average elapsed seconds over started instances, mirroring the row-level
     * {@code (endTime ?? now) - startTime} computation in the read service.
     * Returns {@code null} when no instance has a start time.
     */
    @Query("select avg(timestampdiff(second, i.startTime, coalesce(i.endTime, :now))) "
            + "from FlowInstance i where i.startTime is not null "
            + "and (:assigneeId is null or i.assignedUserId = :assigneeId)")
    Double avgElapsedSeconds(@Param("assigneeId") Long assigneeId,
            @Param("now") java.time.LocalDateTime now);

    /**
     * Average active seconds per instance over the whole scope: total step
     * active seconds divided by the instance count (instances without
     * executions contribute zero, exactly like the row-level sum/count).
     */
    @Query("select (select coalesce(sum(e.activeSeconds), 0) from StepExecution e "
            + "        join FlowInstance owner on owner.id = e.instance.id "
            + "        where (:assigneeId is null or owner.assignedUserId = :assigneeId)) "
            + "       / nullif((select count(i) from FlowInstance i "
            + "        where (:assigneeId is null or i.assignedUserId = :assigneeId)), 0)")
    Double avgActiveSeconds(@Param("assigneeId") Long assigneeId);

    /**
     * Attention candidates only: instances that are still open, or that carry
     * brak history, or that have rework children. A terminal instance with no
     * brak and no children cannot raise any attention flag (STALE requires an
     * open status; OVERDUE needs an expected norm that this pre-filter does not
     * evaluate), so those rows are never built.
     */
    @Query("select i from FlowInstance i where "
            + "(:assigneeId is null or i.assignedUserId = :assigneeId) and ("
            + " i.status not in :terminalStatuses "
            + " or exists (select b.id from BrakEvent b where b.instanceId = i.id) "
            + " or exists (select c.id from FlowInstance c where c.parentInstanceId = i.id))")
    List<FlowInstance> findAttentionCandidates(@Param("assigneeId") Long assigneeId,
            @Param("terminalStatuses") Collection<FlowInstanceStatus> terminalStatuses);

    /**
     * Per-assignee contribution of the rows the candidate pre-filter drops:
     * terminal instances without brak history and without rework children.
     * Returns {@code [assigneeId, instanceCount, activeSecondsSum]} — enough to
     * keep the team workload totals exact without materializing those rows.
     */
    @Query("select i.assignedUserId, count(i), coalesce(sum((select coalesce(sum(e.activeSeconds), 0) "
            + "        from StepExecution e where e.instance.id = i.id)), 0) "
            + "from FlowInstance i "
            + "where i.assignedUserId is not null "
            + "and i.status in :terminalStatuses "
            + "and not exists (select b.id from BrakEvent b where b.instanceId = i.id) "
            + "and not exists (select c.id from FlowInstance c where c.parentInstanceId = i.id) "
            + "group by i.assignedUserId")
    List<Object[]> countTerminalCleanByAssignee(
            @Param("terminalStatuses") Collection<FlowInstanceStatus> terminalStatuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from FlowInstance i where i.id = :id")
    Optional<FlowInstance> findByIdForUpdate(@Param("id") UUID id);
}
