package com.superhumans.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superhumans.auth.JwtAuthenticationFilter;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.controller.TestSecurityHelper;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the {@code server.ssl.enabled} boundary of the generic
 * {@link SecurityConfig}: when HTTPS is enabled, the channel rule applies to
 * every request, including the otherwise public permit-all endpoints, and
 * plain HTTP requests are redirected to HTTPS.
 */
@WebMvcTest(value = SecurityFixtureController.class, properties = "server.ssl.enabled=true")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityFixtureController.class})
class SecurityConfigSslTest {

    @SpringBootConfiguration
    static class TestApplication {}

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
        when(jwtTokenProvider.getRoleFromToken("test-jwt-token")).thenReturn("DOCTOR");
    }

    @Test
    void sslEnabled_plainHttpRequest_toSecuredEndpoint_redirectsToHttps() throws Exception {
        mockMvc.perform(get("/api/private").with(TestSecurityHelper.doctor()))
                .andExpect(status().isFound());
    }

    @Test
    void sslEnabled_plainHttpRequest_evenToPermitAllEndpoint_redirectsToHttps() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isFound());
    }

    @Test
    void sslEnabled_plainHttpRequest_withoutAuthentication_redirectsToHttps() throws Exception {
        mockMvc.perform(get("/api/private"))
                .andExpect(status().isFound());
    }
}
