package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.prosthesismanufacturing.service.FlowTemplateService;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Nested Bean Validation on the template create contract (#410 item 6):
 * constraints on stages/steps/elements must fire through {@code @Valid}.
 */
@WebMvcTest(FlowTemplateController.class)
@EnableTestExceptionHandler
@Import(CurrentUser.class)
@AutoConfigureMockMvc(addFilters = false)
class FlowTemplateControllerValidationTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FlowTemplateService templateService;
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    AuditLogRepository auditLogRepository;
    @MockitoBean
    AuditService auditService;
    @MockitoBean
    com.superhumans.audit.DomainAuditEmitter auditEmitter;

    @Test
    void create_nullStageName_returnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/prosthesis-manufacturing/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TP-UL-01\",\"productType\":\"UPPER_LIMB\","
                                + "\"estimatedDurationMin\":240,"
                                + "\"stages\":[{\"name\":null,\"type\":\"CLINICAL\",\"steps\":[]}]}")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_nullStepType_returnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/prosthesis-manufacturing/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TP-UL-01\",\"productType\":\"UPPER_LIMB\","
                                + "\"estimatedDurationMin\":240,"
                                + "\"stages\":[{\"name\":\"Stage\",\"type\":\"CLINICAL\","
                                + "\"steps\":[{\"name\":\"Step\",\"stepType\":null}]}]}")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
