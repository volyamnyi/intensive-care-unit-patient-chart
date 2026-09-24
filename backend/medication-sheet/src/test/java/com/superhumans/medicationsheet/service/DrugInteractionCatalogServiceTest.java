package com.superhumans.medicationsheet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superhumans.entity.core.SystemSettings;
import com.superhumans.exception.BadRequestException;
import com.superhumans.medicationsheet.dto.DrugInteractionCatalogResponse;
import com.superhumans.medicationsheet.entity.DrugInteractionDrug;
import com.superhumans.medicationsheet.entity.DrugInteractionPair;
import com.superhumans.medicationsheet.repository.DrugInteractionDrugRepository;
import com.superhumans.medicationsheet.repository.DrugInteractionPairRepository;
import com.superhumans.repository.core.SystemSettingsRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for the admin catalog read path (issues #305/#307).
 *
 * <p>Covers the PostgreSQL {@code lower(bytea)} regression: a blank query
 * must reach the repository as an empty string, never NULL.
 */
@ExtendWith(MockitoExtension.class)
class DrugInteractionCatalogServiceTest {

    @Mock
    private DrugInteractionDrugRepository drugRepository;

    @Mock
    private DrugInteractionPairRepository pairRepository;

    @Mock
    private SystemSettingsRepository settingsRepository;

    @InjectMocks
    private DrugInteractionCatalogService catalogService;

    private static final Pageable PAGE = PageRequest.of(0, 50);

    @Test
    void getCatalog_blankQueryPassesEmptyStringNeverNull() {
        when(drugRepository.findAllOrderByAtcCodeAsc()).thenReturn(List.of());
        when(pairRepository.findCatalog(isNull(), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PAGE, 0));
        when(pairRepository.countBySeverity()).thenReturn(List.of());
        when(settingsRepository.findByKey(DrugInteractionImportService.LAST_IMPORT_KEY))
                .thenReturn(Optional.empty());

        DrugInteractionCatalogResponse res = catalogService.getCatalog(null, "   ", PAGE);

        verify(pairRepository).findCatalog(isNull(), eq(""), any(Pageable.class));
        assertThat(res.getSummary().getDrugs()).isZero();
        assertThat(res.getSummary().getInteractions()).isZero();
        assertThat(res.getSummary().getLastImportAt()).isNull();
        assertThat(res.getPage().getContent()).isEmpty();
    }

    @Test
    void getCatalog_trimsAndMapsRows() {
        DrugInteractionDrug drug = DrugInteractionDrug.builder()
                .atcCode("N02BE01").ukrainianRaw("Парацетамол 500 мг").genericEn("Paracetamol").build();
        DrugInteractionPair pair = DrugInteractionPair.builder()
                .drugAAtc("M01AE01").drugBAtc("N02BE01").severity("high")
                .interaction("текст").interactionId("DI-0001").rowHash("h").build();
        when(drugRepository.findAllOrderByAtcCodeAsc()).thenReturn(List.of(drug));
        when(pairRepository.findCatalog(eq("high"), eq("парацетамол"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pair), PAGE, 1));
        when(pairRepository.countBySeverity()).thenReturn(List.<Object[]>of(new Object[]{"high", 1L}));
        when(pairRepository.count()).thenReturn(1L);
        when(settingsRepository.findByKey(DrugInteractionImportService.LAST_IMPORT_KEY))
                .thenReturn(Optional.of(SystemSettings.builder()
                        .key(DrugInteractionImportService.LAST_IMPORT_KEY)
                        .value("2026-09-23T10:00:00")
                        .build()));

        DrugInteractionCatalogResponse res = catalogService.getCatalog("HIGH", "  парацетамол ", PAGE);

        verify(pairRepository).findCatalog(eq("high"), eq("парацетамол"), any(Pageable.class));
        assertThat(res.getDrugs()).hasSize(1);
        assertThat(res.getDrugs().get(0).getUkrainianRaw()).isEqualTo("Парацетамол 500 мг");
        assertThat(res.getPage().getContent()).hasSize(1);
        assertThat(res.getPage().getContent().get(0).getSeverity()).isEqualTo("high");
        assertThat(res.getSummary().getInteractions()).isEqualTo(1L);
        assertThat(res.getSummary().getBySeverity()).isEqualTo(
                Map.of("low", 0L, "medium", 0L, "high", 1L, "critical", 0L));
        assertThat(res.getSummary().getLastImportAt()).isEqualTo("2026-09-23T10:00:00");
    }

    @Test
    void getCatalog_unknownSeverityThrows() {
        assertThatThrownBy(() -> catalogService.getCatalog("unknown", null, PAGE))
                .isInstanceOf(BadRequestException.class);
    }
}
