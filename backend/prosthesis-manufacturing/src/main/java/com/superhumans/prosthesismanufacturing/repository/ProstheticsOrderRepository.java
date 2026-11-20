package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProstheticsOrderRepository extends JpaRepository<ProstheticsOrder, UUID> {
    List<ProstheticsOrder> findByPatientId(String patientId);
    List<ProstheticsOrder> findByPatientIdAndStatus(String patientId, OrderStatus status);
    List<ProstheticsOrder> findByStatus(OrderStatus status);
    Optional<ProstheticsOrder> findByOrderNumber(String orderNumber);

    /**
     * Advisory lock for issue #458's provision race: serializes the
     * find-or-create section of {@code provisionFromMis} on the two confirmed
     * unique keys, acquired in a fixed order so concurrent provisions cannot
     * deadlock. PostgreSQL only; the lock is xact-scoped — released at
     * commit/rollback.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(?1, 0))", nativeQuery = true)
    List<Object> lockAdvisory(String key);

    /**
     * Batch order load with patients for the production read-model — a single
     * query, no per-row lazy patient fetch (N+1 guard).
     */
    @Query("select o from ProstheticsOrder o join fetch o.patient where o.id in :ids")
    List<ProstheticsOrder> findWithPatientByIds(@Param("ids") Collection<UUID> ids);
}
