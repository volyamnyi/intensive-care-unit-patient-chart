package com.superhumans.controller;

import com.superhumans.auth.JwtAuthenticationFilter;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.CorsConfig;
import com.superhumans.config.SecurityConfig;
import com.superhumans.service.IcuCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
@WebMvcTest(IcuCardController.class)
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private IcuCardService icuCardService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    void anonymousUser_shouldGet403() throws Exception {
        mockMvc.perform(get("/api/icu-cards/active")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCanAccessIcuCards() throws Exception {
        mockMvc.perform(get("/api/icu-cards/active")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "NURSE")
    void nurseCanAccessIcuCards() throws Exception {
        mockMvc.perform(get("/api/icu-cards/active")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HEAD_OF_DEPARTMENT")
    void hodCanAccessIcuCards() throws Exception {
        mockMvc.perform(get("/api/icu-cards/active")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessIcuCards() throws Exception {
        mockMvc.perform(get("/api/icu-cards/active")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "NURSE")
    void nurseCannotCreateIcuCard() throws Exception {
        mockMvc.perform(post("/api/icu-cards")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCannotCreateIcuCard() throws Exception {
        mockMvc.perform(post("/api/icu-cards")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }
}
