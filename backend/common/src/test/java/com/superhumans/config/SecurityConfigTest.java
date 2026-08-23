package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superhumans.auth.JwtAuthenticationFilter;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.auth.TokenRevocationService;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.controller.TestSecurityHelper;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for the generic {@link SecurityConfig} filter chain:
 * base permit-all rules, the authenticated fallback, SSL channel behavior
 * (see {@link SecurityConfigSslTest}) and the {@link SecurityRuleContributor}
 * extension point that the config-move refactoring introduced.
 */
@WebMvcTest(SecurityFixtureController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityFixtureController.class})
class SecurityConfigTest {

    @SpringBootConfiguration
    static class TestApplication {}

    @TestConfiguration
    static class ContributorTestConfiguration {

        @Bean
        SecurityRuleContributor adminOnlyRule() {
            return registry -> registry.requestMatchers("/api/admin-only")
                    .hasRole("ADMINISTRATOR");
        }

        @Bean
        SecurityRuleContributor clinicalOnlyRule() {
            return registry -> registry.requestMatchers("/api/doctor-only")
                    .hasAnyRole("DOCTOR", "NURSE");
        }

        @Bean
        SecurityRuleContributor authRestrictingRule() {
            return registry -> registry.requestMatchers(HttpMethod.POST, "/api/auth/restricted")
                    .hasRole("DOCTOR");
        }

        @Bean
        SecurityRuleContributor passiveContributor() {
            return registry -> {
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    @MockitoBean(name = "passiveContributor", reset = MockReset.NONE)
    private SecurityRuleContributor passiveContributor;

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
    void loginEndpoint_isPermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiEndpoint_isPermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocsEndpoints_arePermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void securedEndpoint_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/private"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void securedEndpoint_isAccessibleToAnyAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/private").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/private").with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/private").with(TestSecurityHelper.admin()))
                .andExpect(status().isOk());
    }

    @Test
    void contributorRule_allowsMatchingRole() throws Exception {
        mockMvc.perform(get("/api/admin-only").with(TestSecurityHelper.admin()))
                .andExpect(status().isOk());
    }

    @Test
    void contributorRule_deniesNonMatchingRoles() throws Exception {
        mockMvc.perform(get("/api/admin-only").with(TestSecurityHelper.doctor()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin-only").with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }

    @Test
    void contributorRule_supportsMultipleRoles() throws Exception {
        mockMvc.perform(get("/api/doctor-only").with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/doctor-only").with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/doctor-only").with(TestSecurityHelper.admin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void basePermitAllRules_takePrecedenceOverContributorRules() throws Exception {
        mockMvc.perform(post("/api/auth/restricted"))
                .andExpect(status().isOk());
    }

    @Test
    void allRegisteredContributors_areInvokedWithTheRegistry() throws Exception {
        verify(passiveContributor).contribute(any());
    }

    @Test
    void passwordEncoder_bean_isBCrypt() {
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }
}
