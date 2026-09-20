package com.superhumans.medicationsheet.repository;

import com.superhumans.medicationsheet.entity.DrugInteractionPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DrugInteractionPairRepository extends JpaRepository<DrugInteractionPair, UUID> {

    @Query("SELECT p FROM DrugInteractionPair p WHERE (p.drugAAtc = :atc OR p.drugBAtc = :atc) AND p.severity IN :severities")
    List<DrugInteractionPair> findDangerousForAtc(String atc, List<String> severities);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DrugInteractionPair")
    void deleteAllPairs();
}
