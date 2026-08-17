package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlowTemplateRepository extends JpaRepository<FlowTemplate, UUID> {
    List<FlowTemplate> findByStatus(TemplateStatus status);
    List<FlowTemplate> findByProductTypeAndStatus(ProductType productType, TemplateStatus status);
    boolean existsByNameAndTemplateVersion(String name, Integer templateVersion);
}
