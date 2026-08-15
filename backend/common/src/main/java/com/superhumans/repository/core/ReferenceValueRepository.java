package com.superhumans.repository.core;

import com.superhumans.entity.core.ReferenceValue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReferenceValueRepository extends JpaRepository<ReferenceValue, UUID> {
    List<ReferenceValue> findByTypeOrderBySortOrderAsc(String type);
    List<ReferenceValue> findByIsActiveTrueOrderBySortOrderAsc();
    boolean existsByTypeAndCode(String type, String code);
}
