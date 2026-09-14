package com.superhumans.medicationsheet.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Read-only validation queries for post-import verification (design #291).
 * All prod-table checks are scoped to one import run through
 * {@code import_id_map}, so pre-existing rows never fail a check.
 */
public interface MigrationCheckRepository extends JpaRepository<
        com.superhumans.medicationsheet.entity.PrescriptionDayPart, UUID> {

    @Query(value = "SELECT day_id FROM prescription_day_parts "
            + "GROUP BY day_id HAVING COUNT(*) <> 4 OR COUNT(DISTINCT period) <> 4",
            nativeQuery = true)
    List<UUID> findDayIdsWithBadGrid();

    @Query(value = "SELECT d.id FROM prescription_item_days d "
            + "JOIN prescription_items i ON i.id = d.item_id "
            + "JOIN prescription_day_parts p ON p.day_id = d.id "
            + "WHERE i.list_id IN (SELECT new_id FROM import_id_map "
            + "WHERE run_id = :runId AND old_kind = 'LIST') "
            + "GROUP BY d.id HAVING COUNT(*) <> 4 OR COUNT(DISTINCT p.period) <> 4",
            nativeQuery = true)
    List<UUID> findImportedDayIdsWithBadGrid(@Param("runId") UUID runId);

    @Query(value = "SELECT COUNT(*) FROM prescription_items WHERE list_id IN "
            + "(SELECT new_id FROM import_id_map WHERE run_id = :runId AND old_kind = 'LIST')",
            nativeQuery = true)
    long importedItemCount(@Param("runId") UUID runId);

    @Query(value = "SELECT COUNT(*) FROM prescription_item_days d "
            + "JOIN prescription_items i ON i.id = d.item_id "
            + "WHERE i.list_id IN (SELECT new_id FROM import_id_map "
            + "WHERE run_id = :runId AND old_kind = 'LIST')", nativeQuery = true)
    long importedDayCount(@Param("runId") UUID runId);

    @Query(value = "SELECT COUNT(*) FROM prescription_day_parts p "
            + "JOIN prescription_item_days d ON d.id = p.day_id "
            + "JOIN prescription_items i ON i.id = d.item_id "
            + "WHERE i.list_id IN (SELECT new_id FROM import_id_map "
            + "WHERE run_id = :runId AND old_kind = 'LIST')", nativeQuery = true)
    long importedPartCount(@Param("runId") UUID runId);

    @Query(value = "SELECT COUNT(*) FROM vital_sign_days d "
            + "WHERE d.vital_list_id IN (SELECT new_id FROM import_id_map "
            + "WHERE run_id = :runId AND old_kind = 'VIT_LIST')", nativeQuery = true)
    long importedVitalDayCount(@Param("runId") UUID runId);

    @Query(value = "SELECT COUNT(*) FROM vital_sign_entries e "
            + "WHERE e.day_id IN (SELECT d.id FROM vital_sign_days d "
            + "WHERE d.vital_list_id IN (SELECT new_id FROM import_id_map "
            + "WHERE run_id = :runId AND old_kind = 'VIT_LIST'))", nativeQuery = true)
    long importedVitalEntryCount(@Param("runId") UUID runId);

    @Query(value = "SELECT COUNT(*) FROM vital_sign_entries "
            + "WHERE temperature IS NOT NULL AND (temperature < 34 OR temperature > 42)",
            nativeQuery = true)
    long outOfRangeTemperatureCount();

    @Query(value = "SELECT COUNT(*) FROM vital_sign_entries "
            + "WHERE pain_score IS NOT NULL AND (pain_score < 0 OR pain_score > 10)",
            nativeQuery = true)
    long outOfRangePainCount();

    @Query(value = "SELECT COUNT(*) FROM vital_sign_entries "
            + "WHERE spo2 IS NOT NULL AND (spo2 < 50 OR spo2 > 100)", nativeQuery = true)
    long outOfRangeSpo2Count();

    @Query(value = "SELECT COUNT(*) FROM vital_sign_entries "
            + "WHERE systolic_bp IS NOT NULL AND (systolic_bp < 50 OR systolic_bp > 250)",
            nativeQuery = true)
    long outOfRangeSystolicCount();

    @Query(value = "SELECT COUNT(*) FROM vital_sign_entries "
            + "WHERE diastolic_bp IS NOT NULL AND (diastolic_bp < 30 OR diastolic_bp > 150)",
            nativeQuery = true)
    long outOfRangeDiastolicCount();

    @Query(value = "SELECT COUNT(*) FROM vital_sign_entries "
            + "WHERE pulse IS NOT NULL AND (pulse < 0 OR pulse > 300)", nativeQuery = true)
    long outOfRangePulseCount();

    @Query(value = "SELECT COUNT(*) FROM prescription_lists WHERE status <> 'Saved' "
            + "AND id IN (SELECT new_id FROM import_id_map "
            + "WHERE run_id = :runId AND old_kind = 'LIST')", nativeQuery = true)
    long nonSavedImportedListCount(@Param("runId") UUID runId);

    @Query(value = "SELECT COUNT(*) FROM prescription_items WHERE status <> 'Active' "
            + "AND list_id IN (SELECT new_id FROM import_id_map "
            + "WHERE run_id = :runId AND old_kind = 'LIST')", nativeQuery = true)
    long nonActiveImportedItemCount(@Param("runId") UUID runId);

    @Query(value = "SELECT COUNT(*) FROM prescription_items i "
            + "WHERE NOT EXISTS (SELECT 1 FROM prescription_lists l WHERE l.id = i.list_id)",
            nativeQuery = true)
    long orphanItemCount();

    @Query(value = "SELECT COUNT(*) FROM prescription_item_days d "
            + "WHERE NOT EXISTS (SELECT 1 FROM prescription_items i WHERE i.id = d.item_id)",
            nativeQuery = true)
    long orphanDayCount();

    @Query(value = "SELECT COUNT(*) FROM prescription_day_parts p "
            + "WHERE NOT EXISTS (SELECT 1 FROM prescription_item_days d WHERE d.id = p.day_id)",
            nativeQuery = true)
    long orphanPartCount();

    @Query(value = "SELECT COUNT(*) FROM vital_sign_days d "
            + "WHERE NOT EXISTS (SELECT 1 FROM vital_sign_lists l WHERE l.id = d.vital_list_id)",
            nativeQuery = true)
    long orphanVitalDayCount();

    @Query(value = "SELECT COUNT(*) FROM vital_sign_entries e "
            + "WHERE NOT EXISTS (SELECT 1 FROM vital_sign_days d WHERE d.id = e.day_id)",
            nativeQuery = true)
    long orphanVitalEntryCount();

    @Query(value = "SELECT COUNT(*) FROM vital_sign_lists l "
            + "WHERE NOT EXISTS (SELECT 1 FROM prescription_lists p "
            + "WHERE p.id = l.prescription_list_id)", nativeQuery = true)
    long orphanVitalListCount();
}
