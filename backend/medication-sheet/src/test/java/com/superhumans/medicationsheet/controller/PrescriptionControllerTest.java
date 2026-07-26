package com.superhumans.medicationsheet.controller;

import com.superhumans.medicationsheet.dto.*;
import com.superhumans.medicationsheet.entity.*;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.AllergyMisDTO;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.medicationsheet.service.*;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrescriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrescriptionListService listService;

    @MockBean
    private PrescriptionItemService itemService;

    @MockBean
    private PrescriptionExecutionService executionService;

    @MockBean
    private VitalSignService vitalSignService;

    @MockBean
    private MisService misService;

    @MockBean
    private com.superhumans.auth.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.superhumans.repository.AuditLogRepository auditLogRepository;

    @MockBean
    private AuditService auditService;

    private UUID listId;
    private UUID itemId;
    private UUID dayPartId;
    private PrescriptionList testList;

    @BeforeEach
    void setUp() {
        listId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        dayPartId = UUID.randomUUID();

        testList = PrescriptionList.builder()
                .patientId(1001L)
                .documentName("Test Prescription")
                .status("Saved")
                .build();
        testList.setId(listId);

        when(jwtTokenProvider.validateToken(any())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(any())).thenReturn("testuser");
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(any())).thenReturn(1L);
    }

    @Test
    void getByPatient_returnsList() throws Exception {
        when(listService.getByPatient(1001L)).thenReturn(List.of(testList));

        mockMvc.perform(get("/api/prescriptions")
                        .param("patientId", "1001")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(listId.toString()))
                .andExpect(jsonPath("$[0].patientId").value(1001));
    }

    @Test
    void getById_returnsPrescription() throws Exception {
        when(listService.getById(listId)).thenReturn(testList);

        mockMvc.perform(get("/api/prescriptions/{id}", listId)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listId.toString()))
                .andExpect(jsonPath("$.documentName").value("Test Prescription"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(listService.create(1001L)).thenReturn(testList);

        mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"1001\"}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(1001));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/prescriptions/{id}", listId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void close_returnsPrescription() throws Exception {
        PrescriptionList closed = PrescriptionList.builder()
                .patientId(1001L).status("Finished").build();
        closed.setId(listId);
        when(listService.getById(listId)).thenReturn(closed);

        mockMvc.perform(post("/api/prescriptions/{id}/close", listId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Finished"));
    }

    @Test
    void getItems_returnsList() throws Exception {
        PrescriptionItem item = PrescriptionItem.builder()
                .list(testList).medicineName("Aspirin").status("Active").build();
        item.setId(itemId);
        when(itemService.getByList(listId)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/prescriptions/{listId}/items", listId)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medicineName").value("Aspirin"));
    }

    @Test
    void addItem_returnsCreated() throws Exception {
        PrescriptionItem item = PrescriptionItem.builder()
                .list(testList).medicineName("Paracetamol").status("Active").build();
        item.setId(itemId);
        when(itemService.addItem(eq(listId), eq("Paracetamol"), eq("PO"), eq("BID")))
                .thenReturn(item);

        mockMvc.perform(post("/api/prescriptions/{listId}/items", listId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineName\":\"Paracetamol\",\"medicineMethod\":\"PO\",\"regime\":\"BID\"}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicineName").value("Paracetamol"));
    }

    @Test
    void removeItem_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/prescriptions/items/{itemId}", itemId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void planDose_returnsDayPart() throws Exception {
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg").isPlanned(true).build();
        part.setId(dayPartId);
        PrescriptionItemDay day = PrescriptionItemDay.builder().build();
        day.setId(UUID.randomUUID());
        part.setDay(day);
        when(itemService.planDose(eq(dayPartId), eq("50mg"), any())).thenReturn(part);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/plan", dayPartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dose\":\"50mg\"}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dose").value("50mg"))
                .andExpect(jsonPath("$.isPlanned").value(true));
    }

    @Test
    void executeDose_returnsOk() throws Exception {
        // Execute endpoint requires NURSE role (EXECUTOR_ROLES)
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("NURSE");
        when(jwtTokenProvider.getUserIdFromToken(any())).thenReturn(2L);

        mockMvc.perform(post("/api/prescriptions/day-parts/{dayPartId}/execute", dayPartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualDose\":\"45mg\",\"requires2pAuth\":false}")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllergies_returnsList() throws Exception {
        AllergyMisDTO allergy = new AllergyMisDTO();
        allergy.setPatientId(1001L);
        allergy.setAllergenName("Penicillin");
        allergy.setSourceDocumentId(42);
        when(misService.getPatientAllergies(1001L)).thenReturn(List.of(allergy));

        mockMvc.perform(get("/api/prescriptions/allergies")
                        .param("patientId", "1001")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].allergenName").value("Penicillin"));
    }

    @Test
    void searchMedicineCatalog_returnsList() throws Exception {
        MedicineMisDTO med = new MedicineMisDTO(1L, "Aspirin", 1, "N01");
        when(misService.searchMedicineCatalog("Asp")).thenReturn(List.of(med));

        mockMvc.perform(get("/api/prescriptions/medicine-catalog")
                        .param("keyword", "Asp")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aspirin"));
    }

    @Test
    void create_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"1001\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withNurseRole_returnsForbidden() throws Exception {
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("NURSE");

        mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"1001\"}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }
}
