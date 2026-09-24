package com.superhumans.medicationsheet.repository;

import com.superhumans.medicationsheet.entity.DrugInteractionPair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DrugInteractionPairRepository extends JpaRepository<DrugInteractionPair, UUID> {

    @Query("SELECT p FROM DrugInteractionPair p WHERE (p.drugAAtc = :atc OR p.drugBAtc = :atc) AND p.severity IN :severities")
    List<DrugInteractionPair> findDangerousForAtc(String atc, List<String> severities);

    /**
     * Admin catalog browse (issue #305): paginated pairs with optional
     * severity exact-match and a case-insensitive substring filter over both
     * ATC codes and both drugs' display names.
     *
     * <p>Both filters use empty-string sentinels, never NULL: Hibernate binds
     * a NULL {@code :q} as {@code bytea}, and PostgreSQL rejects
     * {@code LOWER(bytea)} even inside the {@code IS NULL} branch.
     */
    @Query("SELECT p FROM DrugInteractionPair p "
            + "LEFT JOIN DrugInteractionDrug da ON da.atcCode = p.drugAAtc "
            + "LEFT JOIN DrugInteractionDrug db ON db.atcCode = p.drugBAtc "
            + "WHERE (:severity IS NULL OR p.severity = :severity) "
            + "AND (:q = '' OR LOWER(p.drugAAtc) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "  OR LOWER(p.drugBAtc) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "  OR LOWER(COALESCE(da.ukrainianRaw, '')) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "  OR LOWER(COALESCE(db.ukrainianRaw, '')) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "ORDER BY p.drugAAtc, p.drugBAtc")
    Page<DrugInteractionPair> findCatalog(@Param("severity") String severity,
                                          @Param("q") String q, Pageable pageable);

    @Query("SELECT p.severity, COUNT(p) FROM DrugInteractionPair p GROUP BY p.severity")
    List<Object[]> countBySeverity();

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DrugInteractionPair")
    void deleteAllPairs();
}
