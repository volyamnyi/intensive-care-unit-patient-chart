package com.superhumans.controller;

import com.superhumans.auth.JwtAuthenticationFilter;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.CorsConfig;
import com.superhumans.config.SecurityConfig;
import com.superhumans.mis.MISService;
import com.superhumans.mis.dto.PatientDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MISService misService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "DOCTOR")
    void searchPatients_shouldReturnList() throws Exception {
        List<PatientDTO> patients = List.of(
                PatientDTO.builder().patientID(1001).patientName("Петренко Іван")
                        .patientBirthDate(LocalDate.of(1978, 3, 15)).build());
        when(misService.searchPatients(eq("Петренко"), isNull(), isNull())).thenReturn(patients);

        mockMvc.perform(get("/api/patients/search").param("name", "Петренко"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientID").value(1001))
                .andExpect(jsonPath("$[0].patientName").value("Петренко Іван"));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getPatient_shouldReturnPatient_whenExists() throws Exception {
        PatientDTO patient = PatientDTO.builder().patientID(1001)
                .patientName("Петренко Іван").build();
        when(misService.getPatientInfo(1001, null)).thenReturn(patient);

        mockMvc.perform(get("/api/patients/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientID").value(1001));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getPatient_shouldReturn404_whenNotFound() throws Exception {
        when(misService.getPatientInfo(9999, null)).thenReturn(null);
        mockMvc.perform(get("/api/patients/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void searchPatients_byPhone_shouldReturnResults() throws Exception {
        when(misService.searchPatients(isNull(), eq("+380501234567"), isNull()))
                .thenReturn(List.of(PatientDTO.builder().patientID(1001).build()));

        mockMvc.perform(get("/api/patients/search").param("phone", "+380501234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientID").value(1001));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void searchPatients_noMatch_shouldReturnEmpty() throws Exception {
        when(misService.searchPatients(eq("XXXXX"), isNull(), isNull())).thenReturn(List.of());
        mockMvc.perform(get("/api/patients/search").param("name", "XXXXX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anonymousUser_shouldGet403() throws Exception {
        mockMvc.perform(get("/api/patients/search"))
                .andExpect(status().isForbidden());
    }
}
