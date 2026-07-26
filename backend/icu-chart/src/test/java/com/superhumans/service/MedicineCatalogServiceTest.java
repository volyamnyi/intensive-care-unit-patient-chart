package com.superhumans.service;

import com.superhumans.entity.MedicineCatalogCache;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.repository.MedicineCatalogCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicineCatalogServiceTest {

    @Mock
    private MedicineCatalogCacheRepository cacheRepository;

    @Mock
    private MisService misService;

    @InjectMocks
    private MedicineCatalogService service;

    @Captor
    private ArgumentCaptor<MedicineCatalogCache> cacheCaptor;

    // --- search ---

    @Test
    void search_withKeyword_callsContainsIgnoreCase() {
        MedicineCatalogCache item = MedicineCatalogCache.builder()
                .id(1L).name("Aspirin").categoryRef(1).ptgCode("N01").build();
        when(cacheRepository.findByNameContainingIgnoreCase("Asp")).thenReturn(List.of(item));

        var result = service.search("Asp");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Aspirin");
        verify(cacheRepository).findByNameContainingIgnoreCase("Asp");
    }

    @Test
    void search_withNull_returnsAll() {
        MedicineCatalogCache item1 = MedicineCatalogCache.builder().id(1L).name("A").build();
        MedicineCatalogCache item2 = MedicineCatalogCache.builder().id(2L).name("B").build();
        when(cacheRepository.findAll()).thenReturn(List.of(item1, item2));

        var result = service.search(null);

        assertThat(result).hasSize(2);
        verify(cacheRepository).findAll();
    }

    @Test
    void search_withBlank_returnsAll() {
        when(cacheRepository.findAll()).thenReturn(List.of());

        var result = service.search("   ");

        assertThat(result).isEmpty();
        verify(cacheRepository).findAll();
    }

    // --- refreshCache ---

    @Test
    void refreshCache_savesMisResultsToCache() {
        MedicineMisDTO dto1 = new MedicineMisDTO(1L, "Aspirin", 1, "N01");
        MedicineMisDTO dto2 = new MedicineMisDTO(2L, "Paracetamol", 2, "N02");
        when(misService.searchMedicineCatalog("")).thenReturn(List.of(dto1, dto2));
        when(cacheRepository.save(any(MedicineCatalogCache.class))).thenAnswer(inv -> inv.getArgument(0));

        service.refreshCache();

        verify(cacheRepository, times(2)).save(cacheCaptor.capture());
        List<MedicineCatalogCache> saved = new java.util.ArrayList<>();
        for (var inv : mockingDetails(cacheRepository).getInvocations()) {
            if (inv.getMethod().getName().equals("save")) {
                saved.add(inv.getArgument(0));
            }
        }
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getName()).isEqualTo("Aspirin");
        assertThat(saved.get(0).getCachedAt()).isNotNull();
        assertThat(saved.get(1).getName()).isEqualTo("Paracetamol");
    }

    @Test
    void refreshCache_handlesEmptyMisResponse() {
        when(misService.searchMedicineCatalog("")).thenReturn(List.of());

        service.refreshCache();

        verify(cacheRepository, never()).save(any());
    }

    // --- getById ---

    @Test
    void getById_whenFound_returnsEntry() {
        MedicineCatalogCache item = MedicineCatalogCache.builder()
                .id(42L).name("Ibuprofen").categoryRef(3).ptgCode("M01").build();
        when(cacheRepository.findById(42L)).thenReturn(Optional.of(item));

        var result = service.getById(42L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Ibuprofen");
    }

    @Test
    void getById_whenNotFound_returnsNull() {
        when(cacheRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(service.getById(999L)).isNull();
    }
}
