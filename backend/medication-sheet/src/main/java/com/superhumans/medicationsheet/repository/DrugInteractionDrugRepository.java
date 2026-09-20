package com.superhumans.medicationsheet.repository;

import com.superhumans.medicationsheet.entity.DrugInteractionDrug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DrugInteractionDrugRepository extends JpaRepository<DrugInteractionDrug, UUID> {

    @Query("SELECT d FROM DrugInteractionDrug d WHERE d.atcCode = :atcCode")
    java.util.Optional<DrugInteractionDrug> findByAtcCode(@org.springframework.data.repository.query.Param("atcCode") String atcCode);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DrugInteractionDrug")
    void deleteAllDrugs();
}
