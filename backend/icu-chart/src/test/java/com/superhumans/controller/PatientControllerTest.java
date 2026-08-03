package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
@EnableTestExceptionHandler
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MisService misService;

    @Test
    void searchPatients_returnsList() throws Exception {
        when(misService.searchPatients(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk());
    }

    @Test
    void searchPatients_withQuery_returnsList() throws Exception {
        when(misService.searchPatients(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/patients")
                        .param("query", "test"))
                .andExpect(status().isOk());
    }

    @Test
    void getPatient_returnsOk() throws Exception {
        PatientDTO patient = PatientDTO.builder()
                .id(1L)
                .fullName("Test Patient")
                .build();
        when(misService.getPatient(anyLong())).thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fullName").value("Test Patient"));
    }

    @Test
    void getPatient_notFound_returnsNotFound() throws Exception {
        when(misService.getPatient(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/patients/999"))
                .andExpect(status().isNotFound());
    }
}