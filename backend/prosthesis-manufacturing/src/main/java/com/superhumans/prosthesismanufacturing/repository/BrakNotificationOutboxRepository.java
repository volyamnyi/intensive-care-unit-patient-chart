package com.superhumans.prosthesismanufacturing.repository;

import com.superhumans.prosthesismanufacturing.entity.BrakNotificationOutbox;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrakNotificationOutboxRepository
        extends JpaRepository<BrakNotificationOutbox, UUID> {

    Optional<BrakNotificationOutbox> findByBrakEventId(UUID brakEventId);

    /**
     * Row claim for delivery: blocks a concurrent deliverer (eager listener vs
     * scheduled sweep) until the first one commits, which then sees the
     * terminal state and skips. Requires an active transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from BrakNotificationOutbox o where o.brakEventId = :brakEventId")
    Optional<BrakNotificationOutbox> findByBrakEventIdForUpdate(
            @Param("brakEventId") UUID brakEventId);

    /**
     * Sweep window: retryable rows, oldest first. Single-instance scheduler;
     * one sweep never overlaps itself (fixed-delay).
     */
    List<BrakNotificationOutbox> findByStatusInAndAttemptsLessThanOrderByCreatedAtAsc(
            List<BrakNotificationStatus> statuses, int maxAttempts, Pageable pageable);
}
