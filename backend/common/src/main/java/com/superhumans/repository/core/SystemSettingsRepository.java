package com.superhumans.repository.core;

import com.superhumans.entity.core.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, UUID> {
    Optional<SystemSettings> findByKey(String key);
    boolean existsByKey(String key);
}
