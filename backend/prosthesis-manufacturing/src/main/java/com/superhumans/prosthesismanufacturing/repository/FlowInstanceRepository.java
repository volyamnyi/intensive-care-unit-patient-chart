package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlowInstanceRepository extends JpaRepository<FlowInstance, UUID> {
    List<FlowInstance> findByAssignedUserId(Long assignedUserId);
    List<FlowInstance> findByAssignedUserIdAndStatus(Long assignedUserId, FlowInstanceStatus status);
    List<FlowInstance> findByStatus(FlowInstanceStatus status);
    List<FlowInstance> findByOrderId(UUID orderId);
}
