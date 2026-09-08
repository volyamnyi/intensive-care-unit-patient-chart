package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsCandidateResponse;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsPatientResponse;
import com.superhumans.prosthesismanufacturing.service.ProstheticsEligibilityService;
import com.superhumans.prosthesismanufacturing.service.ProstheticsPatientService;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditService;
import com.superhumans.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC slice for the patient registry: the new {@code /candidates} endpoint
 * delegates to {@code ProstheticsEligibilityService} untouched.
 */
@WebMvcTest(ProstheticsPatientController.class)
@EnableTestExceptionHandler
@Import(CurrentUser.class)
@AutoConfigureMockMvc(addFilters = false)
class ProstheticsPatientControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProstheticsPatientService patientService;
    @MockitoBean
    ProstheticsEligibilityService eligibilityService;
    @MockitoBean
    PermissionService permissionService;
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    AuditLogRepository auditLogRepository;
    @MockitoBean
    AuditService auditService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("prosthetist1", 1L,
                        List.of(new SimpleGrantedAuthority("ROLE_PROSTHETIST"))));
        when(permissionService.hasAny(any(String[].class))).thenReturn(true);
    }

    @Test
    void candidates_returnsEligibilityServiceResult() throws Exception {
        when(eligibilityService.getCandidates()).thenReturn(List.of(
                ProstheticsCandidateResponse.builder()
                        .patient(ProstheticsPatientResponse.builder()
                                .id("900001")
                                .departmentId(19L)
                                .build())
                        .orders(List.of())
                        .documents(List.of())
                        .documentsUnknown(false)
                        .build()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/patients/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patient.id").value("900001"))
                .andExpect(jsonPath("$[0].patient.departmentId").value(19))
                .andExpect(jsonPath("$[0].documentsUnknown").value(false));
    }

    @Test
    void search_stillDelegatesToPatientService() throws Exception {
        when(patientService.search("Сніжко")).thenReturn(List.of(
                ProstheticsPatientResponse.builder().id("900001").build()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/patients")
                        .param("query", "Сніжко"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("900001"));
    }
}
