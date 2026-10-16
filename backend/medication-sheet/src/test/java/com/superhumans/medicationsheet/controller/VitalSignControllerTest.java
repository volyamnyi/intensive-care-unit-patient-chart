package com.superhumans.medicationsheet.controller;

import com.superhumans.medicationsheet.dto.VitalSignDayResponse;
import com.superhumans.medicationsheet.dto.VitalSignEntryResponse;
import com.superhumans.medicationsheet.entity.*;
import com.superhumans.medicationsheet.mapper.*;
import com.superhumans.medicationsheet.service.VitalSignService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VitalSignController.class)
@AutoConfigureMockMvc(addFilters = false)
@com.superhumans.config.EnableTestExceptionHandler
@Import({
    VitalSignDayMapperImpl.class,
    VitalSignEntryMapperImpl.class
})
class VitalSignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.superhumans.audit.DomainAuditEmitter auditEmitter;

    @MockitoBean
    private VitalSignService vitalSignService;

    @MockitoBean
    private com.superhumans.auth.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.superhumans.repository.core.AuditLogRepository auditLogRepository;

    @MockitoBean
    private com.superhumans.service.AuditService auditService;

    private UUID prescriptionListId;
    private UUID vitalListId;
    private UUID dayId;
    private UUID entryId;
    private VitalSignList vitalList;
    private VitalSignDay vitalDay;
    private PrescriptionList prescriptionList;

    @BeforeEach
    void setUp() {
        prescriptionListId = UUID.randomUUID();
        vitalListId = UUID.randomUUID();
        dayId = UUID.randomUUID();
        entryId = UUID.randomUUID();

        prescriptionList = PrescriptionList.builder()
                .patientId(1001L).status("Saved").build();
        prescriptionList.setId(prescriptionListId);

        vitalList = VitalSignList.builder()
                .prescriptionList(prescriptionList).build();
        vitalList.setId(vitalListId);

        vitalDay = VitalSignDay.builder()
                .vitalList(vitalList).dayDate(LocalDate.now()).build();
        vitalDay.setId(dayId);

        when(jwtTokenProvider.validateToken(any())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(any())).thenReturn("testuser");
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(any())).thenReturn(1L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getDaysByPrescriptionList_returnsList() throws Exception {
        when(vitalSignService.getOrCreate(prescriptionListId)).thenReturn(vitalList);
        when(vitalSignService.getDays(vitalListId)).thenReturn(List.of(vitalDay));

        mockMvc.perform(get("/api/vital-signs")
                        .param("prescriptionListId", prescriptionListId.toString())
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(dayId.toString()))
                .andExpect(jsonPath("$[0].dayDate").value(LocalDate.now().toString()));
    }

    @Test
    void getEntries_returnsList() throws Exception {
        VitalSignEntry entry = VitalSignEntry.builder()
                .day(vitalDay).period("morning").temperature(36.6).pulse(72).build();
        entry.setId(entryId);
        when(vitalSignService.getEntries(dayId)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/vital-signs/days/{dayId}/entries", dayId)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(entryId.toString()))
                .andExpect(jsonPath("$[0].temperature").value(36.6))
                .andExpect(jsonPath("$[0].pulse").value(72));
    }

    @Test
    void create_returnsEntryForGivenSlot() throws Exception {
        VitalSignEntry saved = VitalSignEntry.builder()
                .day(vitalDay).period("morning").temperature(37.0).pulse(80).build();
        saved.setId(entryId);
        when(vitalSignService.saveEntry(eq(dayId), eq("morning"), any())).thenReturn(saved);

        mockMvc.perform(post("/api/vital-signs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayId\":\"" + dayId + "\",\"period\":\"morning\",\"temperature\":37.0,\"pulse\":80}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperature").value(37.0))
                .andExpect(jsonPath("$.pulse").value(80));
    }

    @Test
    void create_withMissingPeriod_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/vital-signs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayId\":\"" + dayId + "\",\"temperature\":37.0}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // Security tests (401/403/nurse-role) live in SecurityRulesIntegrationTest —
    // the slice runs with addFilters=false, which bypasses the real filter chain.
}
