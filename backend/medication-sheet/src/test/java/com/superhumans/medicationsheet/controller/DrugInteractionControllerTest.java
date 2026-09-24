package com.superhumans.medicationsheet.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.exception.BadRequestException;
import com.superhumans.medicationsheet.dto.DrugInteractionCatalogResponse;
import com.superhumans.medicationsheet.dto.DrugInteractionCatalogResponse.CatalogPage;
import com.superhumans.medicationsheet.dto.DrugInteractionCatalogResponse.DrugRow;
import com.superhumans.medicationsheet.dto.DrugInteractionCatalogResponse.PairRow;
import com.superhumans.medicationsheet.dto.DrugInteractionCatalogResponse.Summary;
import com.superhumans.medicationsheet.service.DrugInteractionCatalogService;
import com.superhumans.medicationsheet.service.DrugInteractionImportService;
import com.superhumans.medicationsheet.service.DrugInteractionWarningService;
import com.superhumans.service.AuditService;
import com.superhumans.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DrugInteractionController.class)
@EnableTestExceptionHandler
@AutoConfigureMockMvc(addFilters = false)
@Import({
    com.superhumans.config.SecurityConfig.class
})
class DrugInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DrugInteractionWarningService warningService;

    @MockitoBean
    private DrugInteractionImportService importService;

    @MockitoBean
    private DrugInteractionCatalogService catalogService;

    @MockitoBean
    private com.superhumans.auth.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.superhumans.repository.core.AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean(name = "permissionService")
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        when(jwtTokenProvider.validateToken(any())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(any())).thenReturn("admin");
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("ADMINISTRATOR");
        when(jwtTokenProvider.getUserIdFromToken(any())).thenReturn(3L);
        when(permissionService.has(anyString())).thenReturn(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private DrugInteractionCatalogResponse catalogWith(int drugs, long interactions,
                                                      long high, List<PairRow> content, long total) {
        return DrugInteractionCatalogResponse.builder()
                .summary(Summary.builder()
                        .drugs(drugs)
                        .interactions(interactions)
                        .bySeverity(Map.of("low", 0L, "medium", 0L, "high", high, "critical", 0L))
                        .lastImportAt("2026-09-23T10:00:00")
                        .build())
                .drugs(List.of(
                        DrugRow.builder().atcCode("N02BE01").ukrainianRaw("Парацетамол 500 мг").genericEn("Paracetamol").build(),
                        DrugRow.builder().atcCode("M01AE01").ukrainianRaw("Ібупрофен 200 мг").genericEn("Ibuprofen").build()))
                .page(CatalogPage.builder()
                        .content(content)
                        .totalElements(total)
                        .totalPages(total == 0 ? 0 : 1)
                        .build())
                .build();
    }

    private PairRow highPair() {
        return PairRow.builder()
                .drugAAtc("M01AE01")
                .drugBAtc("N02BE01")
                .severity("high")
                .interaction("текст взаємодії")
                .interactionId("DI-0001")
                .build();
    }

    @Test
    void getCatalog_adminReturnsSummaryAndPage() throws Exception {
        when(catalogService.getCatalog(eq("high"), isNull(), any(Pageable.class)))
                .thenReturn(catalogWith(2, 1, 1, List.of(highPair()), 1));

        mockMvc.perform(get("/api/admin/drug-interactions")
                        .param("severity", "high")
                        .param("size", "50")
                        .with(TestSecurityHelper.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.drugs").value(2))
                .andExpect(jsonPath("$.summary.interactions").value(1))
                .andExpect(jsonPath("$.summary.bySeverity.high").value(1))
                .andExpect(jsonPath("$.drugs[0].atcCode").value("N02BE01"))
                .andExpect(jsonPath("$.page.content[0].severity").value("high"))
                .andExpect(jsonPath("$.page.content[0].interactionId").value("DI-0001"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getCatalog_adminQueryPassedToService() throws Exception {
        when(catalogService.getCatalog(isNull(), eq("парацетамол"), any(Pageable.class)))
                .thenReturn(catalogWith(2, 1, 1, List.of(highPair()), 1));

        mockMvc.perform(get("/api/admin/drug-interactions")
                        .param("query", "парацетамол")
                        .with(TestSecurityHelper.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(catalogService).getCatalog(isNull(), eq("парацетамол"), any(Pageable.class));
    }

    @Test
    void getCatalog_doctorForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/drug-interactions")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCatalog_emptyDatabaseReturnsZeros() throws Exception {
        when(catalogService.getCatalog(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(catalogWith(0, 0, 0, List.of(), 0));

        mockMvc.perform(get("/api/admin/drug-interactions")
                        .with(TestSecurityHelper.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.drugs").value(0))
                .andExpect(jsonPath("$.summary.interactions").value(0))
                .andExpect(jsonPath("$.page.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void getCatalog_unknownSeverityReturns400() throws Exception {
        when(catalogService.getCatalog(eq("unknown"), isNull(), any(Pageable.class)))
                .thenThrow(new BadRequestException("Невідомий severity фільтра: unknown"));

        mockMvc.perform(get("/api/admin/drug-interactions")
                        .param("severity", "unknown")
                        .with(TestSecurityHelper.admin()))
                .andExpect(status().isBadRequest());
    }
}
