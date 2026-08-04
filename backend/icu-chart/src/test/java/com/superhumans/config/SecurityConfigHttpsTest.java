package com.superhumans.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the {@code server.ssl.enabled} boundary of the common
 * {@link SecurityConfig}: when HTTPS is enabled, the HTTPS redirect
 * ({@code redirectToHttps}) applies to every request, so plain HTTP requests
 * — even to permit-all endpoints like {@code /api/auth/login} — are
 * redirected to HTTPS.
 */
@Disabled("SSL disabled until keystore is configured for CI")
@SpringBootTest(properties = "server.ssl.enabled=true")
@AutoConfigureMockMvc
class SecurityConfigHttpsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void httpRequest_isRedirectedToHttpsWhenSslEnabled() throws Exception {
        mockMvc.perform(get("/api/auth/login")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isFound());
    }
}
