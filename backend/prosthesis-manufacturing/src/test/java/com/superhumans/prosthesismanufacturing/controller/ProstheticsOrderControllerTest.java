package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.service.ProstheticsOrderService;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProstheticsOrderController.class)
@EnableTestExceptionHandler
@Import(CurrentUser.class)
@AutoConfigureMockMvc(addFilters = false)
class ProstheticsOrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProstheticsOrderService orderService;
    @MockitoBean
    ProstheticsOrderRepository orderRepository;
    @MockitoBean
    MisService misService;
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
    }

    @Test
    void documents_returnsLimbOrdersForPatient() throws Exception {
        when(orderService.getAllLowerLimbsOrdersForByPatientId("13373")).thenReturn(List.of(
                DocumentMisDTO.builder()
                        .documentId(681078L)
                        .documentTemplateId(121L)
                        .documentTemplateName("Замовлення на протези нижніх кінцівок")
                        .documentUrl("https://mis.example/document?request=abc")
                        .build()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/orders/documents")
                        .param("patientId", "13373"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentTemplateId").value(121))
                .andExpect(jsonPath("$[0].documentUrl").value("https://mis.example/document?request=abc"));
    }

    @Test
    void documents_emptyList_whenNoLimbOrders() throws Exception {
        when(orderService.getAllLowerLimbsOrdersForByPatientId("13373")).thenReturn(List.of());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/orders/documents")
                        .param("patientId", "13373"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void documents_rejectsNonNumericPatientId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/orders/documents")
                        .param("patientId", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void documents_rejectsMissingPatientId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/prosthesis-manufacturing/orders/documents"))
                .andExpect(status().isBadRequest());
    }
}
