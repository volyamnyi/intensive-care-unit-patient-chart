package com.superhumans.prosthesismanufacturing.repository;


import com.superhumans.prosthesismanufacturing.entity.ResourceUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceUsageRepository extends JpaRepository<ResourceUsage, UUID> {
    List<ResourceUsage> findByInstanceId(UUID instanceId);
}
