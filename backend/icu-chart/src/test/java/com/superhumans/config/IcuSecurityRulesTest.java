package com.superhumans.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.controller.TestSecurityHelper;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that the URL authorization rules moved from the ICU-specific
 * {@code SecurityConfig} into {@link IcuSecurityRules} are still enforced
 * identically through the generic chain contributed by the common module.
 * Every rule is exercised with a positive (200) and a negative (403) role.
 */
@WebMvcTest(IcuRuleFixtureController.class)
@Import({SecurityConfig.class, IcuSecurityRules.class})
@ActiveProfiles("test-security")
class IcuSecurityRulesTest {

    private static final String EPISODE_PATH = "/api/episodes/ep-1";
    private static final String DAY_PATH = "/api/clinical-days/day-1";
    private static final String ORDER_PATH = "/api/orders/ord-1";
    private static final String PRESCRIPTION_PATH = "/api/prescriptions/pre-1";
    private static final String DAY_PART_PATH = "/api/prescriptions/day-parts/dp-1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private AuditService auditService;

    @BeforeEach
    void setUpJwt() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(anyString())).thenReturn("testuser");
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
        when(jwtTokenProvider.getRoleFromToken("test-jwt-token")).thenReturn("DOCTOR");
        when(jwtTokenProvider.getRoleFromToken("test-nurse-token")).thenReturn("NURSE");
        when(jwtTokenProvider.getRoleFromToken("test-admin-token")).thenReturn("ADMINISTRATOR");
        when(jwtTokenProvider.getRoleFromToken("test-hod-token")).thenReturn("HEAD_OF_DEPARTMENT");
    }

    @Test
    void episodeRead_isAccessibleToAllClinicalRoles() throws Exception {
        mockMvc.perform(get(EPISODE_PATH).with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
        mockMvc.perform(get(EPISODE_PATH).with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
        mockMvc.perform(get(EPISODE_PATH).with(TestSecurityHelper.hod()))
                .andExpect(status().isOk());
        mockMvc.perform(get(EPISODE_PATH).with(TestSecurityHelper.admin()))
                .andExpect(status().isOk());
    }

    @Test
    void episodeCreate_requiresPrescriberRole() throws Exception {
        mockMvc.perform(post("/api/episodes").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/episodes").with(TestSecurityHelper.hod()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/episodes").with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/episodes").with(TestSecurityHelper.admin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void clinicalDayCreate_isAccessibleToClinicalRoles() throws Exception {
        mockMvc.perform(post("/api/clinical-days").with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/clinical-days").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void orderCreate_requiresPrescriberRole() throws Exception {
        mockMvc.perform(post(DAY_PATH + "/orders").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
        mockMvc.perform(post(DAY_PATH + "/orders").with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }

    @Test
    void signEndpoint_isAccessibleToClinicalRoles() throws Exception {
        mockMvc.perform(post(DAY_PATH + "/sign/nurse").with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
        mockMvc.perform(post(DAY_PATH + "/sign/nurse").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void orderExecute_isAccessibleToClinicalRoles() throws Exception {
        mockMvc.perform(post(ORDER_PATH + "/execute").with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
        mockMvc.perform(post(ORDER_PATH + "/execute").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void orderCancel_requiresPrescriberRole() throws Exception {
        mockMvc.perform(post(ORDER_PATH + "/cancel").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
        mockMvc.perform(post(ORDER_PATH + "/cancel").with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditEndpoint_isAdministratorOnly() throws Exception {
        mockMvc.perform(get("/api/audit").with(TestSecurityHelper.admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/audit").with(TestSecurityHelper.doctor()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit").with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit").with(TestSecurityHelper.hod()))
                .andExpect(status().isForbidden());
    }

    @Test
    void prescriptionCreate_requiresPrescriberRole() throws Exception {
        mockMvc.perform(post("/api/prescriptions").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/prescriptions").with(TestSecurityHelper.hod()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/prescriptions").with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/prescriptions").with(TestSecurityHelper.admin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void prescriptionItemCreate_requiresPrescriberRole() throws Exception {
        mockMvc.perform(post(PRESCRIPTION_PATH + "/items").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
        mockMvc.perform(post(PRESCRIPTION_PATH + "/items").with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }

    @Test
    void dayPartPlan_requiresPrescriberRole() throws Exception {
        mockMvc.perform(put(DAY_PART_PATH + "/plan").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
        mockMvc.perform(put(DAY_PART_PATH + "/plan").with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }

    @Test
    void dayPartComplete_requiresExecutorRole() throws Exception {
        mockMvc.perform(put(DAY_PART_PATH + "/complete").with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
        mockMvc.perform(put(DAY_PART_PATH + "/complete").with(TestSecurityHelper.hod()))
                .andExpect(status().isOk());
        mockMvc.perform(put(DAY_PART_PATH + "/complete").with(TestSecurityHelper.doctor()))
                .andExpect(status().isForbidden());
    }

    @Test
    void dayPartExecute_requiresExecutorRole() throws Exception {
        mockMvc.perform(post(DAY_PART_PATH + "/execute").with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
        mockMvc.perform(post(DAY_PART_PATH + "/execute").with(TestSecurityHelper.hod()))
                .andExpect(status().isOk());
        mockMvc.perform(post(DAY_PART_PATH + "/execute").with(TestSecurityHelper.doctor()))
                .andExpect(status().isForbidden());
    }

    @Test
    void prescriptionItemDelete_requiresPrescriberRole() throws Exception {
        mockMvc.perform(delete("/api/prescriptions/items/it-1").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/prescriptions/items/it-1").with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }

    @Test
    void prescriptionDelete_requiresPrescriberRole() throws Exception {
        mockMvc.perform(delete(PRESCRIPTION_PATH).with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
        mockMvc.perform(delete(PRESCRIPTION_PATH).with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }

    @Test
    void vitalSigns_isAccessibleToClinicalRoles() throws Exception {
        mockMvc.perform(get("/api/vital-signs").with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/vital-signs").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpoints_isAdministratorOnly() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(TestSecurityHelper.admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/users").with(TestSecurityHelper.doctor()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }

    @Test
    void scales_isAccessibleToClinicalRoles() throws Exception {
        mockMvc.perform(get("/api/scales").with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/scales").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void patientSearch_isAccessibleToClinicalRoles() throws Exception {
        mockMvc.perform(get("/api/patients").with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/patients").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequest_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(EPISODE_PATH))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/audit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonApiPath_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/health").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
    }
}
