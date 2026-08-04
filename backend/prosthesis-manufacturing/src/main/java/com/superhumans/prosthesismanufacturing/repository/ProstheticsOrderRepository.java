package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProstheticsOrderRepository extends JpaRepository<ProstheticsOrder, UUID> {
    List<ProstheticsOrder> findByPatientId(UUID patientId);
    List<ProstheticsOrder> findByPatientIdAndStatus(UUID patientId, OrderStatus status);
    List<ProstheticsOrder> findByStatus(OrderStatus status);
    Optional<ProstheticsOrder> findByOrderNumber(String orderNumber);
}
