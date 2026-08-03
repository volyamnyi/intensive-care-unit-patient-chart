package com.superhumans.controller;

import com.superhumans.dto.MedicalNoteCreateRequest;
import com.superhumans.dto.MedicalNotePatchRequest;
import com.superhumans.dto.MedicalNoteResponse;
import com.superhumans.service.MedicalNoteService;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.superhumans.controller.TestSecurityHelper.doctor;

@WebMvcTest(MedicalNoteController.class)
@Import(com.superhumans.config.SecurityConfig.class)
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
    void setUpJwt() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(anyString())).thenReturn("testuser");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }
    @Test
    void getNotes_returnsList() throws Exception {
        UUID dayId = UUID.randomUUID();
        MedicalNoteResponse response = MedicalNoteResponse.builder()
                .id(UUID.randomUUID())
                .noteType("Progress Note")
                .text("Patient improving")
                .build();

        when(medicalNoteService.getNotesByClinicalDay(dayId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/notes", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].noteType").value("Progress Note"))
                .andExpect(jsonPath("$[0].text").value("Patient improving"));
    }

    @Test
    void getNotes_emptyList_returnsOk() throws Exception {
        UUID dayId = UUID.randomUUID();
        when(medicalNoteService.getNotesByClinicalDay(dayId)).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/notes", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createNote_returnsCreated() throws Exception {
        UUID dayId = UUID.randomUUID();
        MedicalNoteResponse response = MedicalNoteResponse.builder()
                .id(UUID.randomUUID())
                .noteType("Progress Note")
                .text("New note")
                .build();

        when(medicalNoteService.createNote(eq(dayId), any(MedicalNoteCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/notes", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noteType\":\"Progress Note\",\"text\":\"New note\"}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("New note"));
    }

    @Test
    void createNote_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/notes", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateNote_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(medicalNoteService.updateNote(eq(id), any(MedicalNotePatchRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(patch("/api/notes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Updated note\",\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateNote_conflict_returnsError() throws Exception {
        UUID id = UUID.randomUUID();
        when(medicalNoteService.updateNote(eq(id), any(MedicalNotePatchRequest.class), eq(1L)))
                .thenThrow(new com.superhumans.exception.VersionConflictException("conflict"));

        mockMvc.perform(patch("/api/notes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Updated note\",\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isConflict());
    }
}