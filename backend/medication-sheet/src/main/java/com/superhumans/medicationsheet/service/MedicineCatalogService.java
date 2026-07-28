package com.superhumans.medicationsheet.service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.medicationsheet.entity.MedicineCatalogCache;
import com.superhumans.medicationsheet.repository.MedicineCatalogCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicineCatalogService {

    private final MedicineCatalogCacheRepository cacheRepository;
    private final MisService misService;

    @Transactional(readOnly = true)
    public List<MedicineCatalogCache> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return cacheRepository.findAll();
        }
        return cacheRepository.findByNameContainingIgnoreCase(keyword);
    }

    @Transactional
    public void refreshCache() {
        List<MedicineMisDTO> fromMis = misService.searchMedicineCatalog("");
        for (MedicineMisDTO dto : fromMis) {
            MedicineCatalogCache cache = MedicineCatalogCache.builder()
                    .id(dto.getId())
                    .name(dto.getName())
                    .categoryRef(dto.getCategoryRef())
                    .ptgCode(dto.getPtgCode())
                    .cachedAt(LocalDateTime.now())
                    .build();
            cacheRepository.save(cache);
        }
        log.info("Medicine catalog cache refreshed: {} entries", fromMis.size());
    }

    @Transactional(readOnly = true)
    public MedicineCatalogCache getById(Long id) {
        return cacheRepository.findById(id).orElse(null);
    }
}
