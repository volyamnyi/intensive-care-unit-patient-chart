package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlowInstanceRepository extends JpaRepository<FlowInstance, UUID> {
    List<FlowInstance> findByAssignedUserId(Long assignedUserId);
    List<FlowInstance> findByAssignedUserIdAndStatus(Long assignedUserId, FlowInstanceStatus status);
    List<FlowInstance> findByStatus(FlowInstanceStatus status);
    List<FlowInstance> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from FlowInstance i where i.id = :id")
    Optional<FlowInstance> findByIdForUpdate(@Param("id") UUID id);
}
