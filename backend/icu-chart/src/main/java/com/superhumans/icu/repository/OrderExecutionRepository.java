package com.superhumans.icu.repository;

import com.superhumans.icu.entity.OrderExecution;
import com.superhumans.icu.entity.OrderExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderExecutionRepository extends JpaRepository<OrderExecution, UUID> {
    // Deterministic order with id tiebreaker (#411 item 5): feeds the PDF
    // therapy table and fluid-balance hour mapping.
    List<OrderExecution> findByOrderIdOrderByExecutedAtAscIdAsc(UUID orderId);
    List<OrderExecution> findByOrderIdIn(List<UUID> orderIds);
    List<OrderExecution> findByOrderIdAndStatus(UUID orderId, OrderExecutionStatus status);
    List<OrderExecution> findByExecutedAtBetween(LocalDateTime start, LocalDateTime end);
    java.util.Optional<OrderExecution> findByOrderIdAndHour(UUID orderId, Integer hour);
}
