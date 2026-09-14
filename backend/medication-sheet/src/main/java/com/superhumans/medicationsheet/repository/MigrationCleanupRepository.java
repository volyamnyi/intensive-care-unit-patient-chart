package com.superhumans.medicationsheet.repository;

import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Chunked bulk deletes for import rollback, driven by prescription-list IDs
 * (covers merged vitals, shells and any row, mapped or not).
 * Call order is FK-reverse; used only by the migration profile.
 */
public interface MigrationCleanupRepository extends JpaRepository<PrescriptionDayPart, UUID> {

    @Modifying
    @Query(value = "DELETE FROM prescription_day_parts WHERE day_id IN "
            + "(SELECT d.id FROM prescription_item_days d "
            + "JOIN prescription_items i ON i.id = d.item_id "
            + "WHERE i.list_id IN (:ids))", nativeQuery = true)
    int deletePartsByListIds(@Param("ids") List<UUID> ids);

    @Modifying
    @Query(value = "DELETE FROM prescription_item_days WHERE item_id IN "
            + "(SELECT id FROM prescription_items WHERE list_id IN (:ids))",
            nativeQuery = true)
    int deleteItemDaysByListIds(@Param("ids") List<UUID> ids);

    @Modifying
    @Query(value = "DELETE FROM prescription_items WHERE list_id IN (:ids)",
            nativeQuery = true)
    int deleteItemsByListIds(@Param("ids") List<UUID> ids);

    @Modifying
    @Query(value = "DELETE FROM vital_sign_entries WHERE day_id IN "
            + "(SELECT d.id FROM vital_sign_days d "
            + "JOIN vital_sign_lists l ON l.id = d.vital_list_id "
            + "WHERE l.prescription_list_id IN (:ids))", nativeQuery = true)
    int deleteVitalEntriesByListIds(@Param("ids") List<UUID> ids);

    @Modifying
    @Query(value = "DELETE FROM vital_sign_days WHERE vital_list_id IN "
            + "(SELECT id FROM vital_sign_lists WHERE prescription_list_id IN (:ids))",
            nativeQuery = true)
    int deleteVitalDaysByListIds(@Param("ids") List<UUID> ids);

    @Modifying
    @Query(value = "DELETE FROM vital_sign_lists WHERE prescription_list_id IN (:ids)",
            nativeQuery = true)
    int deleteVitalListsByListIds(@Param("ids") List<UUID> ids);

    @Modifying
    @Query("DELETE FROM PrescriptionList l WHERE l.id IN :ids")
    int deletePrescriptionListsByIds(@Param("ids") List<UUID> ids);
}
