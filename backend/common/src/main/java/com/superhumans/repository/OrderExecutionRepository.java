package com.superhumans.repository;

import com.superhumans.entity.OrderExecution;
import com.superhumans.entity.OrderExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderExecutionRepository extends JpaRepository<OrderExecution, UUID> {
    List<OrderExecution> findByOrderId(UUID orderId);
    List<OrderExecution> findByOrderIdAndStatus(UUID orderId, OrderExecutionStatus status);
    List<OrderExecution> findByExecutedAtBetween(LocalDateTime start, LocalDateTime end);
    java.util.Optional<OrderExecution> findByOrderIdAndHour(UUID orderId, Integer hour);
}
