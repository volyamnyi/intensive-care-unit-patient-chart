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
     * Batch brak counts per originating instance for the production read-model.
     * Returns {@code [instanceId, count]} rows; instances without braks are absent.
     */
    @Query("select b.instanceId, count(b) from BrakEvent b "
            + "where b.instanceId in :ids group by b.instanceId")
    List<Object[]> countByInstanceIds(@Param("ids") Collection<UUID> ids);
}
