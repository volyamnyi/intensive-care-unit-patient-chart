package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.TemplateStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateStageRepository extends JpaRepository<TemplateStage, UUID> {
    List<TemplateStage> findByTemplateIdOrderByOrderIndex(UUID templateId);
}
