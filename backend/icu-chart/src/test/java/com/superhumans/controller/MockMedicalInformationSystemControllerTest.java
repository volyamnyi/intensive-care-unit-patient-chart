package com.superhumans.controller;\n\nimport com.superhumans.config.EnableTestExceptionHandler;

import com.superhumans.mis.MockMisServiceImpl;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MockMedicalInformationSystemController.class)\n@EnableTestExceptionHandler
class MockMedicalInformationSystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MockMisServiceImpl mockMisService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;
@BeforeEach
    void setUpJwt() {
        when(jwtTokenProvider.validateToken("test-jwt-token")).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken("test-jwt-token")).thenReturn("doctor1");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken("test-jwt-token")).thenReturn(1L);
    }
    @Test
    void setErrorMode_timeout_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/mis/error-mode")
                        .param("mode", "timeout")
                        .with(csrf())
                        .with(doctor()))
                .andExpect(status().isNoContent());

        verify(mockMisService).setErrorMode("timeout");
    }

    @Test
    void setErrorMode_notFound_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/mis/error-mode")
                        .param("mode", "not_found")
                        .with(csrf())
                        .with(doctor()))
                .andExpect(status().isNoContent());

        verify(mockMisService).setErrorMode("not_found");
    }

    @Test
    void setErrorMode_none_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/mis/error-mode")
                        .param("mode", "none")
                        .with(csrf())
                        .with(doctor()))
                .andExpect(status().isNoContent());

        verify(mockMisService).setErrorMode("none");
    }
}





