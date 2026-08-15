package com.superhumans.controller;

import com.superhumans.repository.core.SystemSettingsRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemSettingsController {

    SystemSettingsRepository systemSettingsRepository;

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, String>> getByKey(@PathVariable String key) {
        return systemSettingsRepository.findByKey(key)
                .map(s -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(Map.of("key", s.getKey(), "value", s.getValue())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
