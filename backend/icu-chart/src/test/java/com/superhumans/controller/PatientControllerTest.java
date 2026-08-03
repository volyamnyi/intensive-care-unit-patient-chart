package com.superhumans.controller;\n\nimport com.superhumans.config.EnableTestExceptionHandler;

import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)\n@EnableTestExceptionHandler
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MisService misService;

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
    void searchPatients_returnsList() throws Exception {
        PatientDTO patient = PatientDTO.builder()
                .id(1L)
                .fullName("Р В РЎСџР В Р’ВµР РЋРІР‚С™Р РЋР вЂљР В Р’ВµР В Р вЂ¦Р В РЎвЂќР В РЎвЂў")
                .build();

        when(misService.searchPatients("Р В РЎСџР В Р’ВµР РЋРІР‚С™Р РЋР вЂљР В Р’ВµР В Р вЂ¦Р В РЎвЂќР В РЎвЂў")).thenReturn(List.of(patient));

        mockMvc.perform(get("/api/patients").param("query", "Р В РЎСџР В Р’ВµР РЋРІР‚С™Р РЋР вЂљР В Р’ВµР В Р вЂ¦Р В РЎвЂќР В РЎвЂў").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Р В РЎСџР В Р’ВµР РЋРІР‚С™Р РЋР вЂљР В Р’ВµР В Р вЂ¦Р В РЎвЂќР В РЎвЂў"));
    }

    @Test
    void searchPatients_withoutQuery_returnsAll() throws Exception {
        PatientDTO patient = PatientDTO.builder()
                .id(1L)
                .fullName("Р В РЎСџР В Р’ВµР РЋРІР‚С™Р РЋР вЂљР В Р’ВµР В Р вЂ¦Р В РЎвЂќР В РЎвЂў")
                .build();

        when(misService.searchPatients(null)).thenReturn(List.of(patient));

        mockMvc.perform(get("/api/patients").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Р В РЎСџР В Р’ВµР РЋРІР‚С™Р РЋР вЂљР В Р’ВµР В Р вЂ¦Р В РЎвЂќР В РЎвЂў"));
    }

    @Test
    void searchPatients_emptyList_returnsOk() throws Exception {
        when(misService.searchPatients("nonexistent")).thenReturn(List.of());

        mockMvc.perform(get("/api/patients").param("query", "nonexistent").with(doctor()).with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPatient_returnsPatient() throws Exception {
        PatientDTO patient = PatientDTO.builder()
                .id(1L)
                .fullName("Р В РЎСџР В Р’ВµР РЋРІР‚С™Р РЋР вЂљР В Р’ВµР В Р вЂ¦Р В РЎвЂќР В РЎвЂў")
                .build();

        when(misService.getPatient(1L)).thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/{id}", 1L).with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Р В РЎСџР В Р’ВµР РЋРІР‚С™Р РЋР вЂљР В Р’ВµР В Р вЂ¦Р В РЎвЂќР В РЎвЂў"));
    }

    @Test
    void getPatient_notFound_returnsNotFound() throws Exception {
        when(misService.getPatient(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/patients/{id}", 999L).with(doctor()))
                .andExpect(status().isNotFound());
    }
}

