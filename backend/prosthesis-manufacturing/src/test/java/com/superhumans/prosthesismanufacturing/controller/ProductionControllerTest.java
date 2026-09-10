package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.ProductionDetailDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionNormativeDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionQuery;
import com.superhumans.prosthesismanufacturing.dto.ProductionSummaryDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionTeamRowDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionWorkItemDto;
import com.superhumans.prosthesismanufacturing.service.ProductionNormativeService;
import com.superhumans.prosthesismanufacturing.service.ProductionReadService;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditService;
import com.superhumans.service.PermissionCatalog;
import com.superhumans.service.PermissionService;
import com.superhumans.config.EnableTestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductionController.class)
@EnableTestExceptionHandler
@Import(CurrentUser.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductionReadService readService;
    @MockitoBean
    ProductionNormativeService normativeService;
    @MockitoBean
    PermissionService permissionService;
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    AuditLogRepository auditLogRepository;
    @MockitoBean
    AuditService auditService;

    UUID instanceId = UUID.randomUUID();
    com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("prosthetist1", 1L,
                        List.of(new SimpleGrantedAuthority("ROLE_PROSTHETIST"))));
    }

    @Test
    void codes_matchCatalog() {
        // Guard against drift: feature code inlines catalog literals (ModuleBoundaryTest
        // forbids importing PermissionCatalog from feature main code).
        assertThat(ProductionController.Codes.VIEW_ALL)
                .isEqualTo(PermissionCatalog.PROSTHETICS_PRODUCTION_VIEW_ALL);
        assertThat(ProductionController.Codes.PATIENT_VIEW)
                .isEqualTo(PermissionCatalog.PROSTHETICS_PRODUCTION_PATIENT_VIEW);
    }

    @Test
    void list_forcesOwnAssigneeWithoutViewAll() throws Exception {
        when(permissionService.has(ProductionController.Codes.VIEW_ALL)).thenReturn(false);
        when(readService.list(any(ProductionQuery.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/production")
                        .param("assigneeId", "99"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductionQuery> captor = ArgumentCaptor.forClass(ProductionQuery.class);
        verify(readService).list(captor.capture());
        assertThat(captor.getValue().getAssigneeId()).isEqualTo(1L);
    }

    @Test
    void list_honorsAssigneeWithViewAll() throws Exception {
        when(permissionService.has(ProductionController.Codes.VIEW_ALL)).thenReturn(true);
        when(readService.list(any(ProductionQuery.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(
                        ProductionWorkItemDto.builder().instanceId(instanceId).build())));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/production")
                        .param("assigneeId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<ProductionQuery> captor = ArgumentCaptor.forClass(ProductionQuery.class);
        verify(readService).list(captor.capture());
        assertThat(captor.getValue().getAssigneeId()).isEqualTo(99L);
    }

    @Test
    void list_rejectsUnknownQuality() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/production")
                        .param("quality", "NOPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void summary_scopesToOwnWithoutViewAll() throws Exception {
        when(permissionService.has(ProductionController.Codes.VIEW_ALL)).thenReturn(false);
        when(readService.summary(1L)).thenReturn(
                ProductionSummaryDto.builder().totalItems(2).inWork(2).build());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/production/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));

        verify(readService).summary(1L);
    }

    @Test
    void summary_teamScopeWithViewAll() throws Exception {
        when(permissionService.has(ProductionController.Codes.VIEW_ALL)).thenReturn(true);
        when(readService.summary(null)).thenReturn(
                ProductionSummaryDto.builder().totalItems(9).build());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/production/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(9));

        verify(readService).summary(null);
    }

    @Test
    void normative_getAndUpdate() throws Exception {
        when(normativeService.get()).thenReturn(
                new ProductionNormativeService.Normative(1.5, 7));
        when(normativeService.update(eq(2.0), eq(3), eq(1L))).thenReturn(
                new ProductionNormativeService.Normative(2.0, 3));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/production/settings/normative"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueMultiplier").value(1.5))
                .andExpect(jsonPath("$.staleDays").value(7));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/prosthesis-manufacturing/production/settings/normative")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("overdueMultiplier", 2.0, "staleDays", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueMultiplier").value(2.0));

        verify(normativeService).update(eq(2.0), eq(3), eq(1L));
    }

    @Test
    void team_returnsOk() throws Exception {        when(readService.team()).thenReturn(List.of(
                ProductionTeamRowDto.builder().userId(1L).fullName("Іваненко").inWork(2).build()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/production/team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Іваненко"));
    }

    @Test
    void detail_passesViewFlags() throws Exception {
        when(permissionService.has(ProductionController.Codes.VIEW_ALL)).thenReturn(true);
        when(permissionService.has(ProductionController.Codes.PATIENT_VIEW)).thenReturn(false);
        when(readService.detail(eq(instanceId), eq(1L), eq(true), eq(false)))
                .thenReturn(ProductionDetailDto.builder()
                        .workItem(ProductionWorkItemDto.builder().instanceId(instanceId).build())
                        .patientDetailsVisible(false)
                        .build());

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/prosthesis-manufacturing/production/{id}", instanceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientDetailsVisible").value(false));

        verify(readService).detail(eq(instanceId), eq(1L), eq(true), eq(false));
    }

    @Test
    void detail_unknownId_returns404() throws Exception {
        when(readService.detail(eq(instanceId), eq(1L), eq(false), eq(false)))
                .thenThrow(new NotFoundException("Instance not found: " + instanceId));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/prosthesis-manufacturing/production/{id}", instanceId))
                .andExpect(status().isNotFound());
    }
}
