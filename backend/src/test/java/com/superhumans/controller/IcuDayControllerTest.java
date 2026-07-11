package com.superhumans.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.CorsConfig;
import com.superhumans.config.SecurityConfig;
import com.superhumans.dto.ScaleRequest;
import com.superhumans.dto.VitalSignsRequest;
import com.superhumans.entity.*;
import com.superhumans.exception.BadRequestException;
import com.superhumans.repository.UserRepository;
import com.superhumans.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, CorsConfig.class, com.superhumans.auth.JwtAuthenticationFilter.class, com.superhumans.exception.GlobalExceptionHandler.class})
@WebMvcTest(IcuDayController.class)
class IcuDayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IcuDayService icuDayService;

    @MockBean
    private VitalSignService vitalSignService;

    @MockBean
    private FluidBalanceService fluidBalanceService;

    @MockBean
    private ScaleService scaleService;

    @MockBean
    private PdfGeneratorService pdfGeneratorService;

    @MockBean
    private IcuCardService icuCardService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private com.superhumans.repository.FluidIntakeRepository fluidIntakeRepository;

    @MockBean
    private com.superhumans.repository.FluidOutputRepository fluidOutputRepository;

    @MockBean
    private com.superhumans.repository.IcuDayRepository icuDayRepository;

    @MockBean
    private ClinicalNoteService clinicalNoteService;

    @MockBean
    private CareMeasureService careMeasureService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getDaysByCard_shouldReturnList() throws Exception {
        when(icuDayService.getDaysByCard(1L)).thenReturn(List.of(
                IcuDay.builder().id(1L).dayNumber(1).date(LocalDate.now()).build()
        ));

        mockMvc.perform(get("/api/icu-days/by-card/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].dayNumber").value(1));
    }

    @Test
    @WithMockUser(roles = "NURSE")
    void saveVitals_shouldReturnSavedVital() throws Exception {
        HourlyVital vital = HourlyVital.builder()
                .id(1L).hour(10).heartRate(80).systolicBp(120).build();
        when(vitalSignService.saveVitals(eq(1L), eq(10), any())).thenReturn(vital);

        VitalSignsRequest req = new VitalSignsRequest();
        req.setHeartRate(80);
        req.setSystolicBp(120);

        mockMvc.perform(put("/api/icu-days/1/vitals/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hour").value(10))
                .andExpect(jsonPath("$.heartRate").value(80));
    }

    @Test
    @WithMockUser(roles = "NURSE")
    void getVitals_shouldReturnList() throws Exception {
        when(vitalSignService.getVitalsByDay(1L)).thenReturn(List.of(
                HourlyVital.builder().id(1L).hour(0).build(),
                HourlyVital.builder().id(2L).hour(1).build()
        ));

        mockMvc.perform(get("/api/icu-days/1/vitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getBalance_shouldReturnFluidBalance() throws Exception {
        var balance = new FluidBalanceService.FluidBalanceResponse(1L, 800, 500, 300, 300);
        when(fluidBalanceService.getBalance(1L)).thenReturn(balance);

        mockMvc.perform(get("/api/icu-days/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIntake").value(800))
                .andExpect(jsonPath("$.totalOutput").value(500))
                .andExpect(jsonPath("$.dailyBalance").value(300));
    }

    @Test
    @WithMockUser(username = "doctor1", roles = "DOCTOR")
    void saveScale_shouldReturnAssessment() throws Exception {
        ScaleAssessment assessment = ScaleAssessment.builder()
                .id(1L).scaleType(ScaleType.APACHE_II).score(25).hour(8).assessedBy("doctor1").build();
        when(scaleService.saveScale(eq(1L), any(), eq("doctor1"))).thenReturn(assessment);

        ScaleRequest req = new ScaleRequest();
        req.setScaleType("APACHE_II");
        req.setScore(25);
        req.setHour(8);

        mockMvc.perform(post("/api/icu-days/1/scales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scaleType").value("APACHE_II"))
                .andExpect(jsonPath("$.score").value(25));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getScales_shouldReturnList() throws Exception {
        when(scaleService.getScalesByDay(1L)).thenReturn(List.of(
                ScaleAssessment.builder().id(1L).scaleType(ScaleType.SOFA).score(5).build()
        ));

        mockMvc.perform(get("/api/icu-days/1/scales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scaleType").value("SOFA"));
    }

    @Test
    @WithMockUser(username = "doctor1", roles = "DOCTOR")
    void signOff_shouldSignDay() throws Exception {
        User doctor = User.builder().id(1L).login("doctor1").build();
        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(doctor));

        IcuDay signedDay = IcuDay.builder()
                .id(1L).dayNumber(1).status(DayStatus.SIGNED).doctorId(1L).build();
        when(icuDayService.signOff(1L, 1L, "doctor1")).thenReturn(signedDay);

        mockMvc.perform(post("/api/icu-days/1/sign-off"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED"));
    }

    @Test
    @WithMockUser(username = "doctor1", roles = "DOCTOR")
    void saveVitals_hourOutOfRange_shouldReturnBadRequest() throws Exception {
        when(vitalSignService.saveVitals(eq(1L), eq(24), any()))
                .thenThrow(new BadRequestException("Hour must be between 0 and 23"));

        VitalSignsRequest req = new VitalSignsRequest();
        req.setHeartRate(80);

        mockMvc.perform(put("/api/icu-days/1/vitals/24")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getDaysByCard_emptyList_shouldReturnEmptyArray() throws Exception {
        when(icuDayService.getDaysByCard(999L)).thenReturn(List.of());

        mockMvc.perform(get("/api/icu-days/by-card/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void generatePdf_shouldReturnPdf() throws Exception {
        IcuDay day = IcuDay.builder().id(1L).build();
        IcuCard card = IcuCard.builder().id(1L).build();
        when(icuDayService.getDay(1L)).thenReturn(day);
        when(pdfGeneratorService.generateDayPdf(day, card)).thenReturn(new byte[]{37, 80, 68, 70});

        mockMvc.perform(get("/api/icu-days/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=icu-day-1.pdf"));
    }
}
