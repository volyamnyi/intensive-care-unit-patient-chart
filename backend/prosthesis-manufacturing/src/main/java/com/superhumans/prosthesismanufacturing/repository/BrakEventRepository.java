package com.superhumans.prosthesismanufacturing.repository;

import com.superhumans.prosthesismanufacturing.entity.BrakEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface BrakEventRepository extends JpaRepository<BrakEvent, UUID> {
    List<BrakEvent> findByInstanceId(UUID instanceId);
    List<BrakEvent> findByNewInstanceId(UUID newInstanceId);

    /**
     * Order-chain brak count for the threshold escalation (epic #322): number
     * of brak events whose originating instance belongs to the given order.
     * Every brak spawns a new branch instance, so per-instance counts never
     * exceed 1 — the chain must be counted via the shared orderId.
     */
    @Query("select count(b) from BrakEvent b where b.instanceId in "
            + "(select f.id from FlowInstance f where f.orderId = :orderId)")
    long countByOrderId(@Param("orderId") UUID orderId);

    /**
     * Chain count as of a triggering event (epic #322, issue #327): delivery
     * of a {@code THRESHOLD} row may lag behind later braks of the same
     * order, so the count backing the "Брак №N" subject must only include
     * events confirmed no later than the triggering event itself.
     */
    @Query("select count(b) from BrakEvent b where b.instanceId in "
            + "(select f.id from FlowInstance f where f.orderId = :orderId) "
            + "and b.createdAt <= :cutoff")
    long countByOrderIdUpTo(@Param("orderId") UUID orderId,
            @Param("cutoff") java.time.LocalDateTime cutoff);

    /**
     * Batch brak counts per originating instance for the production read-model.
     * Returns {@code [instanceId, count]} rows; instances without braks are absent.
     */
    @Query("select b.instanceId, count(b) from BrakEvent b "
            + "where b.instanceId in :ids group by b.instanceId")
    List<Object[]> countByInstanceIds(@Param("ids") Collection<UUID> ids);
}
