package com.superhumans.prosthesismanufacturing.repository;

import com.superhumans.prosthesismanufacturing.entity.BrakNotificationKind;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationOutbox;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrakNotificationOutboxRepository
        extends JpaRepository<BrakNotificationOutbox, UUID> {

    Optional<BrakNotificationOutbox> findByBrakEventIdAndKind(
            UUID brakEventId, BrakNotificationKind kind);

    @Modifying
    @Transactional(transactionManager = "prosthTransactionManager")
    @Query(value = "INSERT INTO prosthetics_brak_notifications "
            + "(id, brak_event_id, kind, order_id, status, attempts, created_by, updated_by) "
            + "VALUES (:id, :brakEventId, 'THRESHOLD', :orderId, 'PENDING', 0, "
            + "COALESCE(:actorId, 0), COALESCE(:actorId, 0)) "
            + "ON CONFLICT (brak_event_id, kind) DO NOTHING", nativeQuery = true)
    int insertThresholdIfAbsent(@Param("id") UUID id,
            @Param("brakEventId") UUID brakEventId,
            @Param("orderId") UUID orderId,
            @Param("actorId") Long actorId);

    List<BrakNotificationOutbox> findAllByBrakEventId(UUID brakEventId);

    /**
     * Row claim for delivery: blocks a concurrent deliverer (eager listener vs
     * scheduled sweep) until the first one commits, which then sees the
     * terminal state and skips. Requires an active transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(transactionManager = "prosthTransactionManager")
    @Query("select o from BrakNotificationOutbox o "
            + "where o.brakEventId = :brakEventId and o.kind = :kind")
    Optional<BrakNotificationOutbox> findByBrakEventIdAndKindForUpdate(
            @Param("brakEventId") UUID brakEventId,
            @Param("kind") BrakNotificationKind kind);

    @Modifying
    @Transactional(transactionManager = "prosthTransactionManager")
    @Query("update BrakNotificationOutbox o set o.status = :processing, "
            + "o.leaseUntil = :leaseUntil, o.claimedBy = :claimedBy "
            + "where o.id = :id and ("
            + "o.status in :retryable or "
            + "(o.status = :processing and o.leaseUntil < :now))")
    int claimForDelivery(@Param("id") UUID id,
            @Param("processing") BrakNotificationStatus processing,
            @Param("retryable") List<BrakNotificationStatus> retryable,
            @Param("now") java.time.LocalDateTime now,
            @Param("leaseUntil") java.time.LocalDateTime leaseUntil,
            @Param("claimedBy") String claimedBy);

    @Modifying
    @Transactional(transactionManager = "prosthTransactionManager")
    @Query("update BrakNotificationOutbox o set o.status = :status, "
            + "o.attempts = :attempts, o.lastError = :lastError, "
            + "o.leaseUntil = null, o.claimedBy = null "
            + "where o.id = :id and o.status = :processing")
    int recordDelivery(@Param("id") UUID id,
            @Param("status") BrakNotificationStatus status,
            @Param("attempts") int attempts,
            @Param("lastError") String lastError,
            @Param("processing") BrakNotificationStatus processing);

    /**
     * Sweep window: retryable rows, oldest first. Single-instance scheduler;
     * one sweep never overlaps itself (fixed-delay).
     */
    @Query("select o from BrakNotificationOutbox o where "
            + "(o.status in :statuses and o.attempts < :maxAttempts) or "
            + "(o.status = :processing and o.leaseUntil < CURRENT_TIMESTAMP) "
            + "order by o.createdAt asc")
    List<BrakNotificationOutbox> findDueForDelivery(
            @Param("statuses") List<BrakNotificationStatus> statuses,
            @Param("processing") BrakNotificationStatus processing,
            @Param("maxAttempts") int maxAttempts, Pageable pageable);

    /** Legacy query retained for existing repository consumers and fixtures. */
    List<BrakNotificationOutbox> findByStatusInAndAttemptsLessThanOrderByCreatedAtAsc(
            List<BrakNotificationStatus> statuses, int maxAttempts, Pageable pageable);
}
