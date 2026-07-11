package com.superhumans.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.CorsConfig;
import com.superhumans.config.SecurityConfig;
import com.superhumans.dto.IcuCardCreateRequest;
import com.superhumans.entity.CardStatus;
import com.superhumans.entity.IcuCard;
import com.superhumans.service.IcuCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, CorsConfig.class, com.superhumans.auth.JwtAuthenticationFilter.class})
@WebMvcTest(IcuCardController.class)
class IcuCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IcuCardService icuCardService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "DOCTOR")
    void createCard_shouldReturnCard() throws Exception {
        IcuCard card = IcuCard.builder()
                .id(1L)
                .patientName("Test Patient")
                .diagnosis("Test")
                .status(CardStatus.ACTIVE)
                .build();

        when(icuCardService.createCard(anyLong(), anyString(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(card);

        IcuCardCreateRequest req = new IcuCardCreateRequest();
        req.setPatientId(100L);
        req.setPatientName("Test Patient");
        req.setMedicalCardNumber("MC-001");
        req.setDiagnosis("Test Diagnosis");
        req.setApacheIi(15);
        req.setSofa(8);

        mockMvc.perform(post("/api/icu-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientName").value("Test Patient"));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void createCard_withNullOptionalFields_shouldSucceed() throws Exception {
        IcuCard card = IcuCard.builder()
                .id(2L)
                .patientName("Test Patient")
                .diagnosis("Test")
                .status(CardStatus.ACTIVE)
                .build();

        when(icuCardService.createCard(anyLong(), anyString(), anyString(), anyString(), isNull(), isNull(), anyString()))
                .thenReturn(card);

        IcuCardCreateRequest req = new IcuCardCreateRequest();
        req.setPatientId(100L);
        req.setPatientName("Test Patient");
        req.setMedicalCardNumber("MC-001");
        req.setDiagnosis("Test Diagnosis");

        mockMvc.perform(post("/api/icu-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.patientName").value("Test Patient"));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getCard_shouldReturnCard() throws Exception {
        IcuCard card = IcuCard.builder()
                .id(1L)
                .patientName("Patient")
                .build();
        when(icuCardService.getCard(1L)).thenReturn(card);

        mockMvc.perform(get("/api/icu-cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientName").value("Patient"));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getActiveCards_shouldReturnList() throws Exception {
        List<IcuCard> cards = List.of(
                IcuCard.builder().id(1L).patientName("Patient 1").build(),
                IcuCard.builder().id(2L).patientName("Patient 2").build()
        );
        when(icuCardService.getActiveCards()).thenReturn(cards);

        mockMvc.perform(get("/api/icu-cards/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getCardsByPatient_shouldReturnList() throws Exception {
        List<IcuCard> cards = List.of(
                IcuCard.builder().id(1L).build()
        );
        when(icuCardService.getCardsByPatient(100L)).thenReturn(cards);

        mockMvc.perform(get("/api/icu-cards/by-patient/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
