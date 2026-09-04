package com.superhumans.medicationsheet.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.medicationsheet.dto.*;
import com.superhumans.medicationsheet.entity.*;
import com.superhumans.medicationsheet.mapper.*;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.AllergyMisDTO;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.exception.BusinessException;
import com.superhumans.exception.ErrorCode;
import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.service.*;
import com.superhumans.service.AuditService;
import com.superhumans.service.PermissionService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrescriptionController.class)
@EnableTestExceptionHandler
@AutoConfigureMockMvc(addFilters = false)
@Import({
    com.superhumans.config.SecurityConfig.class,
    PrescriptionListMapperImpl.class,
    PrescriptionItemMapperImpl.class,
    PrescriptionDayPartMapperImpl.class,
    AllergyMapperImpl.class,
    MedicineCatalogMapperImpl.class
})
class PrescriptionControllerTest {

    private static final String TEST_DOCTOR_LOGIN = "testuser";
    private static final String TEST_NURSE_LOGIN = "nurse2";
    private static final String TEST_ADMIN_LOGIN = "admin";
    private static final Long TEST_DOCTOR_ID = 1L;
    private static final Long TEST_NURSE_ID = 2L;
    private static final Long TEST_HOD_ID = 4L;
    private static final String TEST_PATIENT_ID = "1001";
    private static final String TEST_MEDICINE_NAME = "Aspirin";
    private static final String TEST_DOSE = "50mg";
    private static final String TEST_ACTUAL_DOSE = "45mg";
    private static final String TEST_SECOND_PERSON_PASSWORD = "nurse123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrescriptionListService listService;

    @MockitoBean
    private PrescriptionItemService itemService;

    @MockitoBean
    private PrescriptionExecutionService executionService;

    @MockitoBean
    private VitalSignService vitalSignService;

    @MockitoBean
    private MisService misService;

    @MockitoBean
    private com.superhumans.auth.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.superhumans.repository.core.AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean(name = "permissionService")
    private PermissionService permissionService;

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
        when(jwtTokenProvider.getLoginFromToken(any())).thenReturn(TEST_DOCTOR_LOGIN);
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(any())).thenReturn(TEST_DOCTOR_ID);
        when(permissionService.has(anyString())).thenReturn(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getByPatient_returnsList() throws Exception {
        when(listService.getByPatient(1001L)).thenReturn(List.of(testList));

        mockMvc.perform(get("/api/prescriptions")
                        .param("patientId", TEST_PATIENT_ID)
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
                        .content("{\"patientId\":\"" + TEST_PATIENT_ID + "\"}")
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

    private PrescriptionItem itemWithDayAndParts(String medicineName) {
        PrescriptionItem item = PrescriptionItem.builder()
                .list(testList).medicineName(medicineName).status("Active").build();
        item.setId(itemId);

        PrescriptionItemDay day = PrescriptionItemDay.builder()
                .item(item)
                .dayDate(LocalDate.parse("2026-01-15"))
                .build();
        day.setId(UUID.randomUUID());

        for (String period : List.of("morning", "day", "evening", "night")) {
            PrescriptionDayPart part = PrescriptionDayPart.builder()
                    .day(day).period(period).dose("50mg")
                    .isPlanned(true).isPlannedFinished(false)
                    .isCompleted(false).isCompletedFinished(false)
                    .build();
            part.setId(UUID.randomUUID());
            day.getDayParts().add(part);
        }
        item.getDays().add(day);
        return item;
    }

    @Test
    void addDay_returnsCreated_withUpdatedItem() throws Exception {
        PrescriptionItem item = itemWithDayAndParts("Paracetamol");
        when(itemService.getListItem(eq(itemId))).thenReturn(item);

        mockMvc.perform(post("/api/prescriptions/items/{itemId}/days", itemId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.listId").value(listId.toString()))
                .andExpect(jsonPath("$.medicineName").value("Paracetamol"))
                .andExpect(jsonPath("$.dayParts").isArray())
                .andExpect(jsonPath("$.dayParts.length()").value(4))
                .andExpect(jsonPath("$.dayParts[0].period").value("morning"));

        verify(itemService).addDay(itemId);
        verify(itemService).getListItem(itemId);
    }

    @Test
    void addDay_withHodRole_returnsCreated() throws Exception {
        PrescriptionItem item = itemWithDayAndParts("Paracetamol");
        when(itemService.getListItem(eq(itemId))).thenReturn(item);

        mockMvc.perform(post("/api/prescriptions/items/{itemId}/days", itemId)
                        .with(TestSecurityHelper.hod()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(itemId.toString()));
    }

    @Test
    void addDay_itemNotFound_returnsNotFound() throws Exception {
        when(itemService.addDay(eq(itemId)))
                .thenThrow(new NotFoundException("Item not found: " + itemId));

        mockMvc.perform(post("/api/prescriptions/items/{itemId}/days", itemId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Item not found: " + itemId));
    }

    @Test
    void removeDay_returnsNoContent() throws Exception {
        UUID dayId = UUID.randomUUID();
        mockMvc.perform(delete("/api/prescriptions/items/{itemId}/days/{dayId}", itemId, dayId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNoContent());

        verify(itemService).removeDay(itemId, dayId);
    }

    @Test
    void removeDay_withHodRole_returnsNoContent() throws Exception {
        UUID dayId = UUID.randomUUID();
        mockMvc.perform(delete("/api/prescriptions/items/{itemId}/days/{dayId}", itemId, dayId)
                        .with(TestSecurityHelper.hod()))
                .andExpect(status().isNoContent());

        verify(itemService).removeDay(itemId, dayId);
    }

    @Test
    void removeDay_dayNotFound_returnsNotFound() throws Exception {
        UUID dayId = UUID.randomUUID();
        doThrow(new NotFoundException("Day not found: " + dayId))
                .when(itemService).removeDay(eq(itemId), eq(dayId));

        mockMvc.perform(delete("/api/prescriptions/items/{itemId}/days/{dayId}", itemId, dayId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Day not found: " + dayId));
    }

    @Test
    void removeDay_withExecutedParts_returnsUnprocessableEntity() throws Exception {
        UUID dayId = UUID.randomUUID();
        String ukMsg = "Р”РµРЅСЊ РјС–СЃС‚РёС‚СЊ РІРёРєРѕРЅР°РЅС– РїСЂРёР·РЅР°С‡РµРЅРЅСЏ, РІРёРґР°Р»РµРЅРЅСЏ РЅРµРјРѕР¶Р»РёРІРµ";
        doThrow(new BusinessException(ErrorCode.BUSINESS_RULE, ukMsg))
                .when(itemService).removeDay(eq(itemId), eq(dayId));

        mockMvc.perform(delete("/api/prescriptions/items/{itemId}/days/{dayId}", itemId, dayId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.BUSINESS_RULE))
                .andExpect(jsonPath("$.message").value(ukMsg));
    }

    @Test
    void planDose_returnsDayPart() throws Exception {
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg").isPlanned(true).build();
        part.setId(dayPartId);
        PrescriptionItemDay day = PrescriptionItemDay.builder().build();
        day.setId(UUID.randomUUID());
        part.setDay(day);
        when(itemService.planDose(eq(dayPartId), eq("50mg"), any(), any())).thenReturn(part);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/plan", dayPartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dose\":\"50mg\"}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dose").value("50mg"))
                .andExpect(jsonPath("$.isPlanned").value(true));
    }

    @Test
    void planDose_generatesDeterministicUuidFromAuthenticatedUser() throws Exception {
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg").isPlanned(true).build();
        part.setId(dayPartId);
        when(itemService.planDose(eq(dayPartId), eq("50mg"), any(), any())).thenReturn(part);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/plan", dayPartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dose\":\"50mg\"}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());

        UUID expectedUuid = UUID.nameUUIDFromBytes("1".getBytes());
        verify(itemService).planDose(eq(dayPartId), eq("50mg"), eq(expectedUuid), eq(1L));
    }

    @Test
    void completeDose_generatesDeterministicUuidFromAuthenticatedUser() throws Exception {
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg").isPlanned(true).build();
        part.setId(dayPartId);
        when(itemService.markCompleted(eq(dayPartId), any())).thenReturn(part);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/complete", dayPartId)
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());

        UUID expectedUuid = UUID.nameUUIDFromBytes("2".getBytes());
        verify(itemService).markCompleted(eq(dayPartId), eq(expectedUuid));
    }

    @Test
    void cancelDose_generatesDeterministicUuidFromAuthenticatedUser() throws Exception {
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg").isPlanned(true).isPlannedFinished(true).build();
        part.setId(dayPartId);
        when(itemService.markPlannedFinished(eq(dayPartId), any(), any())).thenReturn(part);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/cancel", dayPartId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk());

        UUID expectedUuid = UUID.nameUUIDFromBytes("1".getBytes());
        verify(itemService).markPlannedFinished(eq(dayPartId), eq(expectedUuid), eq(1L));
    }

    @Test
    void executeDose_returnsOk() throws Exception {
        mockMvc.perform(post("/api/prescriptions/day-parts/{dayPartId}/execute", dayPartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualDose\":\"" + TEST_ACTUAL_DOSE + "\",\"secondPersonLogin\":\"" + TEST_NURSE_LOGIN + "\",\"secondPersonPassword\":\"" + TEST_SECOND_PERSON_PASSWORD + "\"}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());

        verify(executionService).execute(eq(dayPartId), eq(TEST_NURSE_ID), eq("user"),
                eq(TEST_ACTUAL_DOSE), eq(TEST_NURSE_LOGIN), eq(TEST_SECOND_PERSON_PASSWORD));
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

    // --- Role-based access tests ---

    @Test
    void getByPatient_withNurseRole_returnsOk() throws Exception {
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("NURSE");
        when(listService.getByPatient(1001L)).thenReturn(List.of(testList));

        mockMvc.perform(get("/api/prescriptions")
                        .param("patientId", "1001")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
    }

    @Test
    void getByPatient_withHodRole_returnsOk() throws Exception {
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("HEAD_OF_DEPARTMENT");
        when(listService.getByPatient(1001L)).thenReturn(List.of(testList));

        mockMvc.perform(get("/api/prescriptions")
                        .param("patientId", "1001")
                        .with(TestSecurityHelper.hod()))
                .andExpect(status().isOk());
    }

    @Test
    void create_withHodRole_returnsCreated() throws Exception {
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("HEAD_OF_DEPARTMENT");
        when(listService.create(1001L)).thenReturn(testList);

        mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"1001\"}")
                        .with(TestSecurityHelper.hod()))
                .andExpect(status().isCreated());
    }

    @Test
    void delete_withHodRole_returnsNoContent() throws Exception {
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("HEAD_OF_DEPARTMENT");

        mockMvc.perform(delete("/api/prescriptions/{id}", listId)
                        .with(TestSecurityHelper.hod()))
                .andExpect(status().isNoContent());
    }

    @Test
    void close_withHodRole_returnsPrescription() throws Exception {
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("HEAD_OF_DEPARTMENT");
        PrescriptionList closed = PrescriptionList.builder()
                .patientId(1001L).status("Finished").build();
        closed.setId(listId);
        when(listService.getById(listId)).thenReturn(closed);

        mockMvc.perform(post("/api/prescriptions/{id}/close", listId)
                        .with(TestSecurityHelper.hod()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Finished"));
    }

    @Test
    void planDose_withHodRole_returnsDayPart() throws Exception {
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("HEAD_OF_DEPARTMENT");
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg").isPlanned(true).build();
        part.setId(dayPartId);
        when(itemService.planDose(eq(dayPartId), eq("50mg"), any(), any())).thenReturn(part);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/plan", dayPartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dose\":\"50mg\"}")
                        .with(TestSecurityHelper.hod()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dose").value("50mg"));
    }

    @Test
    void completeDose_withNurseRole_returnsOk() throws Exception {
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("NURSE");
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg").isPlanned(true).build();
        part.setId(dayPartId);
        when(itemService.markCompleted(eq(dayPartId), any())).thenReturn(part);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/complete", dayPartId)
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isOk());
    }

    @Test
    void completeDose_withHodRole_returnsOk() throws Exception {
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("HEAD_OF_DEPARTMENT");
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg").isPlanned(true).build();
        part.setId(dayPartId);
        when(itemService.markCompleted(eq(dayPartId), any())).thenReturn(part);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/complete", dayPartId)
                        .with(TestSecurityHelper.hod()))
                .andExpect(status().isOk());
    }

    @Test
    void cancelDose_withHodRole_returnsDayPart() throws Exception {
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("HEAD_OF_DEPARTMENT");
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg").isPlanned(true).isPlannedFinished(true).build();
        part.setId(dayPartId);
        when(itemService.markPlannedFinished(eq(dayPartId), any(), any())).thenReturn(part);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/cancel", dayPartId)
                        .with(TestSecurityHelper.hod()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPlannedFinished").value(true));
    }

    @Test
    void replanDose_returnsDayPart() throws Exception {
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg").isPlanned(true).isPlannedFinished(false).build();
        part.setId(dayPartId);
        when(itemService.restoreToPlanned(eq(dayPartId), any(), any())).thenReturn(part);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/replan", dayPartId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPlanned").value(true))
                .andExpect(jsonPath("$.isPlannedFinished").value(false))
                .andExpect(jsonPath("$.dose").value("50mg"));

        UUID expectedUuid = UUID.nameUUIDFromBytes("1".getBytes());
        verify(itemService).restoreToPlanned(eq(dayPartId), eq(expectedUuid), eq(1L));
    }

    @Test
    void replanDose_withHodRole_returnsDayPart() throws Exception {
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("HEAD_OF_DEPARTMENT");
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .period("morning").dose("50mg").isPlanned(true).isPlannedFinished(false).build();
        part.setId(dayPartId);
        when(itemService.restoreToPlanned(eq(dayPartId), any(), any())).thenReturn(part);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/replan", dayPartId)
                        .with(TestSecurityHelper.hod()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPlannedFinished").value(false));
    }

    @Test
    void replanDose_withNurseRole_returnsForbidden() throws Exception {
        when(permissionService.has(eq("PRESCRIPTION_CREATE"))).thenReturn(false);
        when(permissionService.has(eq("PRESCRIPTION_EXECUTE"))).thenReturn(true);

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/replan", dayPartId)
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());

        verify(itemService, never()).restoreToPlanned(any(), any(), any());
    }

    @Test
    void replanDose_partNotFound_returnsNotFound() throws Exception {
        when(itemService.restoreToPlanned(eq(dayPartId), any(), any()))
                .thenThrow(new NotFoundException("Day part not found: " + dayPartId));

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/replan", dayPartId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Day part not found: " + dayPartId));
    }

    @Test
    void replanDose_notCancelled_returnsUnprocessableEntity() throws Exception {
        String ukMsg = "Призначення не у статусі «Відмінено», повернення неможливе";
        when(itemService.restoreToPlanned(eq(dayPartId), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.BUSINESS_RULE, ukMsg));

        mockMvc.perform(put("/api/prescriptions/day-parts/{dayPartId}/replan", dayPartId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.BUSINESS_RULE))
                .andExpect(jsonPath("$.message").value(ukMsg));
    }

    @Test
    void executeDose_withHodRole_returnsOk() throws Exception {
        mockMvc.perform(post("/api/prescriptions/day-parts/{dayPartId}/execute", dayPartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualDose\":\"45mg\",\"secondPersonLogin\":\"nurse2\",\"secondPersonPassword\":\"nurse123\"}")
                        .with(TestSecurityHelper.hod()))
                .andExpect(status().isOk());

        verify(executionService).execute(eq(dayPartId), eq(4L), eq("user"),
                eq("45mg"), eq("nurse2"), eq("nurse123"));
    }

    @Test
    void addDay_withNurseRole_returnsForbidden() throws Exception {
        when(permissionService.has(eq("PRESCRIPTION_CREATE"))).thenReturn(false);
        when(permissionService.has(eq("PRESCRIPTION_EXECUTE"))).thenReturn(true);

        mockMvc.perform(post("/api/prescriptions/items/{itemId}/days", itemId)
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());

        verify(itemService, never()).addDay(any());
    }

    @Test
    void removeDay_withNurseRole_returnsForbidden() throws Exception {
        UUID dayId = UUID.randomUUID();
        when(permissionService.has(eq("PRESCRIPTION_CREATE"))).thenReturn(false);
        when(permissionService.has(eq("PRESCRIPTION_EXECUTE"))).thenReturn(true);

        mockMvc.perform(delete("/api/prescriptions/items/{itemId}/days/{dayId}", itemId, dayId)
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());

        verify(itemService, never()).removeDay(any(), any());
    }
}
