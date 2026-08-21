package com.superhumans.medicationsheet.repository;


import com.superhumans.medicationsheet.entity.PrescriptionItem;
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
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {
    List<PrescriptionItem> findByListId(UUID listId);
    List<PrescriptionItem> findByListIdOrderBySortOrderAsc(UUID listId);
    List<PrescriptionItem> findByListIdAndDeletedFalseOrderBySortOrderAsc(UUID listId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PrescriptionItem i where i.id = :id")
    Optional<PrescriptionItem> findByIdForUpdate(@Param("id") UUID id);
}
