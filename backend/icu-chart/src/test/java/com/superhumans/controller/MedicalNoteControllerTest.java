package com.superhumans.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.MedicalNoteCreateRequest;
import com.superhumans.dto.MedicalNotePatchRequest;
import com.superhumans.dto.MedicalNoteResponse;
import com.superhumans.service.MedicalNoteService;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicalNoteController.class)
@EnableTestExceptionHandler
class MedicalNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicalNoteService medicalNoteService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        when(jwtTokenProvider.validateToken(any())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(any())).thenReturn("user");
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(any())).thenReturn(1L);
    }

    @Test
    void create_returnsCreated() throws Exception {
        MedicalNoteResponse response = MedicalNoteResponse.builder().id(UUID.randomUUID()).build();
        when(medicalNoteService.createNote(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noteType\":\"Progress Note\",\"text\":\"New note\"}").with(csrf()).with(doctor()))
                .andExpect(status().isCreated());
    }

    @Test
    void getByClinicalDay_returnsList() throws Exception {
        when(medicalNoteService.getNotesByClinicalDay(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/notes").with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void patch_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/notes/123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}").with(csrf()).with(doctor()))
                .andExpect(status().isNoContent());
    }
}