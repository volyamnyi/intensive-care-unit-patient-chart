package com.superhumans.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "server.ssl.enabled=true")
@AutoConfigureMockMvc
class SecurityConfigHttpsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void httpRequest_isRejectedWhenSslEnabled() throws Exception {
        mockMvc.perform(get("/api/auth/login")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
    }
}
