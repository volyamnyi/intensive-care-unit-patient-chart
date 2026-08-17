package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.TemplateStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateStepRepository extends JpaRepository<TemplateStep, UUID> {
    List<TemplateStep> findByStageIdOrderByOrderIndex(UUID stageId);
}
