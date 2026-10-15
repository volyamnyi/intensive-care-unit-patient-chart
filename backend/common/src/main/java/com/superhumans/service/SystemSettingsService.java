package com.superhumans.service;

import com.superhumans.repository.core.SystemSettingsRepository;
import com.superhumans.entity.core.SystemSettings;
import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Read access to {@code system_settings} for the settings endpoint (#424). */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemSettingsService {

    SystemSettingsRepository systemSettingsRepository;

    @Transactional(readOnly = true)
    public Optional<SystemSettings> findByKey(String key) {
        return systemSettingsRepository.findByKey(key);
    }
}
