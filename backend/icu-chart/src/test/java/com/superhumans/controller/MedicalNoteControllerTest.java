package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.MedicalNoteCreateRequest;
import com.superhumans.dto.MedicalNotePatchRequest;
import com.superhumans.dto.MedicalNoteResponse;
import com.superhumans.service.MedicalNoteService;
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

    @Test
    void create_returnsCreated() throws Exception {
        MedicalNoteResponse response = MedicalNoteResponse.builder().id(UUID.randomUUID()).build();
        when(medicalNoteService.createNote(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Test\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getByClinicalDay_returnsList() throws Exception {
        when(medicalNoteService.getNotesByClinicalDay(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/notes"))
                .andExpect(status().isOk());
    }

    @Test
    void patch_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/notes/123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isNoContent());
    }
}