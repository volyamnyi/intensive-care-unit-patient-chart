package com.superhumans.repository;

import com.superhumans.entity.DrugInteractionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DrugInteractionRuleRepository extends JpaRepository<DrugInteractionRule, UUID> {
    @Query("SELECT r FROM DrugInteractionRule r WHERE r.ptgCodeA = :code OR r.ptgCodeB = :code")
    List<DrugInteractionRule> findConflictsForPtgCode(String code);
}
