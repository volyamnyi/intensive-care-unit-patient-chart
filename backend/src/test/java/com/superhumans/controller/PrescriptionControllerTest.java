package com.superhumans.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.auth.JwtAuthenticationFilter;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.CorsConfig;
import com.superhumans.config.SecurityConfig;
import com.superhumans.dto.PrescriptionRequest;
import com.superhumans.entity.*;
import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.GlobalExceptionHandler;
import com.superhumans.repository.UserRepository;
import com.superhumans.service.PrescriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@WebMvcTest(PrescriptionController.class)
class PrescriptionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private PrescriptionService prescriptionService;
    @MockBean private UserRepository userRepository;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "doctor1", roles = "DOCTOR")
    void createPrescription_shouldReturnPrescription() throws Exception {
        User doctor = User.builder().id(1L).login("doctor1").build();
        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(doctor));
        Prescription prescription = Prescription.builder().id(1L).medication("Dopamine")
                .dose("200 mg").status(PrescriptionStatus.ACTIVE).build();
        when(prescriptionService.createPrescription(eq(1L), any(), eq(1L), eq("doctor1")))
                .thenReturn(prescription);

        PrescriptionRequest req = new PrescriptionRequest();
        req.setMedication("Dopamine");
        req.setDose("200 mg");
        req.setRoute("IV");
        req.setFrequency("q4h");
        req.setStartHour(0);
        req.setEndHour(23);

        mockMvc.perform(post("/api/prescriptions/by-card/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medication").value("Dopamine"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getCardPrescriptions_shouldReturnList() throws Exception {
        when(prescriptionService.getCardPrescriptions(1L)).thenReturn(List.of(
                Prescription.builder().id(1L).medication("Dopamine").build()));
        mockMvc.perform(get("/api/prescriptions/by-card/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medication").value("Dopamine"));
    }

    @Test
    @WithMockUser(username = "doctor1", roles = "DOCTOR")
    void stopPrescription_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/prescriptions/1/stop"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "nurse1", roles = "NURSE")
    void executePrescription_shouldReturnFluidIntake() throws Exception {
        FluidIntake intake = FluidIntake.builder().id(1L).hour(10).volumeActual(180)
                .medicationName("Dopamine").status(ExecutionStatus.DONE).build();
        when(prescriptionService.executePrescription(eq(1L), eq(5L), eq(10), eq(180), eq("nurse1")))
                .thenReturn(intake);

        Map<String, Object> body = Map.of("dayId", 5, "hour", 10, "actualVolume", 180);
        mockMvc.perform(post("/api/prescriptions/1/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hour").value(10))
                .andExpect(jsonPath("$.volumeActual").value(180));
    }

    @Test
    @WithMockUser(username = "nurse1", roles = "NURSE")
    void executePrescription_stoppedPrescription_shouldReturn400() throws Exception {
        when(prescriptionService.executePrescription(eq(1L), eq(5L), eq(10), eq(180), eq("nurse1")))
                .thenThrow(new BadRequestException("Cannot execute stopped prescription"));

        Map<String, Object> body = Map.of("dayId", 5, "hour", 10, "actualVolume", 180);
        mockMvc.perform(post("/api/prescriptions/1/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "nurse1", roles = "NURSE")
    void executePrescription_partialVolume_shouldSucceed() throws Exception {
        FluidIntake intake = FluidIntake.builder().id(2L).hour(10).volumeActual(80)
                .medicationName("Dopamine").status(ExecutionStatus.DONE).build();
        when(prescriptionService.executePrescription(eq(1L), eq(5L), eq(10), eq(80), eq("nurse1")))
                .thenReturn(intake);

        Map<String, Object> body = Map.of("dayId", 5, "hour", 10, "actualVolume", 80);
        mockMvc.perform(post("/api/prescriptions/1/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volumeActual").value(80));
    }

    @Test
    @WithMockUser(roles = "NURSE")
    void nurse_shouldGet403_onCreatePrescription() throws Exception {
        mockMvc.perform(post("/api/prescriptions/by-card/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
