package com.superhumans.integration;

import com.superhumans.medicationsheet.dto.*;
import com.superhumans.medicationsheet.entity.*;
import com.superhumans.medicationsheet.repository.*;
import com.superhumans.medicationsheet.service.PrescriptionItemService;
import com.superhumans.medicationsheet.service.PrescriptionListService;
import com.superhumans.repository.core.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PrescriptionListRepository listRepository;
    @Autowired
    private PrescriptionItemRepository itemRepository;
    @Autowired
    private PrescriptionItemDayRepository dayRepository;
    @Autowired
    private PrescriptionDayPartRepository partRepository;
    @Autowired
    private PrescriptionExecutionRepository executionRepository;
    @Autowired
    private PrescriptionItemService itemService;
    @Autowired
    private PrescriptionListService listService;
    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final UUID SEED_LIST_ID =
            UUID.fromString("cccc0001-0001-0001-0001-000000000001");
    private static final UUID FINISHED_LIST_ID =
            UUID.fromString("cccc0002-0002-0002-0002-000000000002");
    private static final UUID SEED_ITEM_ID =
            UUID.fromString("dddd0001-0001-0001-0001-000000000001");
    private static final UUID SEED_DAY_ID =
            UUID.fromString("eeee0001-0001-0001-0001-000000000001");
    private static final UUID SEED_MORNING_PART_ID =
            UUID.fromString("ffff0001-0001-0001-0001-000000000001");
    private static final UUID SEED_DAY_PART_ID =
            UUID.fromString("ffff0002-0002-0002-0002-000000000002");

    @Test
    void getByPatient_returnsSeedList() {
        var res = restTemplate.exchange(
                "/api/prescriptions?patientId=1001", HttpMethod.GET,
                authGet(getDoctorToken()),
                new ParameterizedTypeReference<List<PrescriptionListResponse>>() {});

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull().isNotEmpty();
        assertThat(res.getBody().get(0).getId()).isEqualTo(SEED_LIST_ID);
    }

    @Test
    void getById_returnsSeedPrescription() {
        var res = restTemplate.exchange(
                "/api/prescriptions/{id}", HttpMethod.GET,
                authGet(getDoctorToken()),
                PrescriptionListResponse.class, SEED_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getId()).isEqualTo(SEED_LIST_ID);
    }

    @Test
    void create_asDoctor_returnsCreated() {
        PrescriptionListCreateRequest req = new PrescriptionListCreateRequest();
        req.setPatientId("1003");

        var res = restTemplate.exchange(
                "/api/prescriptions", HttpMethod.POST,
                authEntity(req, getDoctorToken()),
                PrescriptionListResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
    }

    @Test
    void create_asNurse_returnsForbidden() {
        PrescriptionListCreateRequest req = new PrescriptionListCreateRequest();
        req.setPatientId("1004");

        var res = restTemplate.exchange(
                "/api/prescriptions", HttpMethod.POST,
                authEntity(req, getNurseToken()),
                String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void close_asDoctor_returnsOk() {
        var res = restTemplate.exchange(
                "/api/prescriptions/{id}/close", HttpMethod.POST,
                authEntity(null, getDoctorToken()),
                PrescriptionListResponse.class, SEED_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getStatus()).isEqualTo("Finished");
    }

    @Test
    void close_asNurse_returnsForbidden() {
        var res = restTemplate.exchange(
                "/api/prescriptions/{id}/close", HttpMethod.POST,
                authEntity(null, getNurseToken()),
                String.class, SEED_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_asDoctor_returnsNoContent() {
        var res = restTemplate.exchange(
                "/api/prescriptions/{id}", HttpMethod.DELETE,
                authEntity(null, getDoctorToken()),
                String.class, FINISHED_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void delete_asNurse_returnsForbidden() {
        var res = restTemplate.exchange(
                "/api/prescriptions/{id}", HttpMethod.DELETE,
                authEntity(null, getNurseToken()),
                String.class, SEED_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getItems_returnsSeedItem() {
        var res = restTemplate.exchange(
                "/api/prescriptions/{listId}/items", HttpMethod.GET,
                authGet(getDoctorToken()),
                new ParameterizedTypeReference<List<PrescriptionItemResponse>>() {},
                SEED_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull().hasSize(1);
        assertThat(res.getBody().get(0).getMedicineName()).isEqualTo("Aspirin");
    }

    @Test
    void addItem_asDoctor_returnsCreated() {
        PrescriptionItemAddRequest req = new PrescriptionItemAddRequest();
        req.setMedicineName("Paracetamol");
        req.setMedicineMethod("PO");
        req.setRegime("TID");

        var res = restTemplate.exchange(
                "/api/prescriptions/{listId}/items", HttpMethod.POST,
                authEntity(req, getDoctorToken()),
                PrescriptionItemResponse.class, SEED_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getMedicineName()).isEqualTo("Paracetamol");
    }

    @Test
    void addItem_asNurse_returnsForbidden() {
        PrescriptionItemAddRequest req = new PrescriptionItemAddRequest();
        req.setMedicineName("Ibuprofen");
        req.setMedicineMethod("PO");
        req.setRegime("BID");

        var res = restTemplate.exchange(
                "/api/prescriptions/{listId}/items", HttpMethod.POST,
                authEntity(req, getNurseToken()),
                String.class, SEED_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void removeItem_asDoctor_returnsNoContent() {
        var res = restTemplate.exchange(
                "/api/prescriptions/items/{itemId}", HttpMethod.DELETE,
                authEntity(null, getDoctorToken()),
                String.class, SEED_ITEM_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void planDose_asDoctor_returnsOk() {
        PrescriptionDoseRequest req = new PrescriptionDoseRequest();
        req.setDose("100mg");

        var res = restTemplate.exchange(
                "/api/prescriptions/day-parts/{dayPartId}/plan", HttpMethod.PUT,
                authEntity(req, getDoctorToken()),
                PrescriptionDayPartResponse.class, SEED_MORNING_PART_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getIsPlanned()).isTrue();
    }

    @Test
    void planDose_asNurse_returnsForbidden() {
        PrescriptionDoseRequest req = new PrescriptionDoseRequest();
        req.setDose("50mg");

        var res = restTemplate.exchange(
                "/api/prescriptions/day-parts/{dayPartId}/plan", HttpMethod.PUT,
                authEntity(req, getNurseToken()),
                String.class, SEED_DAY_PART_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void executeDose_asNurse_returnsOk() {
        PrescriptionDoseRequest planReq = new PrescriptionDoseRequest();
        planReq.setDose("30mg");
        restTemplate.exchange(
                "/api/prescriptions/day-parts/{dayPartId}/plan", HttpMethod.PUT,
                authEntity(planReq, getDoctorToken()),
                PrescriptionDayPartResponse.class, SEED_DAY_PART_ID);

        PrescriptionExecuteRequest execReq = new PrescriptionExecuteRequest();
        execReq.setActualDose("30mg");
        execReq.setSecondPersonLogin("nurse2");
        execReq.setSecondPersonPassword("nurse123");

        var res = restTemplate.exchange(
                "/api/prescriptions/day-parts/{dayPartId}/execute", HttpMethod.POST,
                authEntity(execReq, getNurseToken()),
                String.class, SEED_DAY_PART_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void executeDose_asDoctor_returnsForbidden() {
        PrescriptionExecuteRequest req = new PrescriptionExecuteRequest();
        req.setActualDose("25mg");
        req.setSecondPersonLogin("nurse2");
        req.setSecondPersonPassword("nurse123");

        var res = restTemplate.exchange(
                "/api/prescriptions/day-parts/{dayPartId}/execute", HttpMethod.POST,
                authEntity(req, getDoctorToken()),
                String.class, SEED_DAY_PART_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getItems_returnsEmptyForFinishedList() {
        var res = restTemplate.exchange(
                "/api/prescriptions/{listId}/items", HttpMethod.GET,
                authGet(getDoctorToken()),
                new ParameterizedTypeReference<List<PrescriptionItemResponse>>() {},
                FINISHED_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull().isEmpty();
    }

    // --- Phase 2–4 flows: HTTP → Service → DB → AuditLog (isolated chains) ---

    private UUID newItemId() {
        PrescriptionList list = listService.create(1003L);
        return itemService.addItem(list.getId(), "IT-Drug", "IV", "test").getId();
    }

    private PrescriptionDayPart morningPartOf(UUID itemId, LocalDate dayDate) {
        return itemService.getDays(itemId).stream()
                .filter(d -> dayDate.equals(d.getDayDate()))
                .findFirst().orElseThrow()
                .getDayParts().stream()
                .filter(p -> "morning".equals(p.getPeriod()))
                .findFirst().orElseThrow();
    }

    private boolean hasAudit(String entity, UUID entityId, String action) {
        return auditLogRepository
                .findByEntityAndEntityIdOrderByTimestampDesc(entity, entityId, PageRequest.of(0, 10))
                .stream().anyMatch(a -> action.equals(a.getAction()) && a.getUserId() != null);
    }

    @Test
    void cancelDose_asDoctor_cancelsAndWritesAudit() {
        UUID itemId = newItemId();
        PrescriptionDayPart part = morningPartOf(itemId, LocalDate.now());
        itemService.planDose(part.getId(), "50mg", UUID.randomUUID(), 1L);

        var res = restTemplate.exchange(
                "/api/prescriptions/day-parts/{dayPartId}/cancel", HttpMethod.PUT,
                authEntity(null, getDoctorToken()),
                PrescriptionDayPartResponse.class, part.getId());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getIsPlannedFinished()).isTrue();

        PrescriptionDayPart persisted = partRepository.findById(part.getId()).orElseThrow();
        assertThat(persisted.getIsPlanned()).isTrue();
        assertThat(persisted.getIsPlannedFinished()).isTrue();
        assertThat(persisted.getDose()).isEqualTo("50mg");
        assertThat(hasAudit("PrescriptionDayPart", part.getId(), "CANCEL")).isTrue();
    }

    @Test
    void cancelDose_asNurse_returnsForbidden() {
        UUID itemId = newItemId();
        PrescriptionDayPart part = morningPartOf(itemId, LocalDate.now());
        itemService.planDose(part.getId(), "50mg", UUID.randomUUID(), 1L);

        var res = restTemplate.exchange(
                "/api/prescriptions/day-parts/{dayPartId}/cancel", HttpMethod.PUT,
                authEntity(null, getNurseToken()),
                String.class, part.getId());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void replanDose_asDoctor_restoresAndWritesAudit() {
        UUID itemId = newItemId();
        PrescriptionDayPart part = morningPartOf(itemId, LocalDate.now());
        itemService.planDose(part.getId(), "50mg", UUID.randomUUID(), 1L);
        itemService.markPlannedFinished(part.getId(), UUID.randomUUID(), 1L);

        var res = restTemplate.exchange(
                "/api/prescriptions/day-parts/{dayPartId}/replan", HttpMethod.PUT,
                authEntity(null, getDoctorToken()),
                PrescriptionDayPartResponse.class, part.getId());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getIsPlanned()).isTrue();
        assertThat(res.getBody().getIsPlannedFinished()).isFalse();
        assertThat(res.getBody().getDose()).isEqualTo("50mg");

        PrescriptionDayPart persisted = partRepository.findById(part.getId()).orElseThrow();
        assertThat(persisted.getIsPlannedFinished()).isFalse();
        assertThat(hasAudit("PrescriptionDayPart", part.getId(), "REPLAN")).isTrue();
    }

    @Test
    void replanDose_notCancelled_returnsUnprocessableEntity() {
        UUID itemId = newItemId();
        PrescriptionDayPart part = morningPartOf(itemId, LocalDate.now());
        itemService.planDose(part.getId(), "50mg", UUID.randomUUID(), 1L);

        var res = restTemplate.exchange(
                "/api/prescriptions/day-parts/{dayPartId}/replan", HttpMethod.PUT,
                authEntity(null, getDoctorToken()),
                String.class, part.getId());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void cancelAssignment_asDoctor_resetsCellKeepsSiblingAndWritesAudit() {
        UUID itemId = newItemId();
        LocalDate today = LocalDate.now();
        PrescriptionDayPart target = morningPartOf(itemId, today);
        PrescriptionDayPart sibling = itemService.getDays(itemId).stream()
                .filter(d -> today.equals(d.getDayDate())).findFirst().orElseThrow()
                .getDayParts().stream()
                .filter(p -> "day".equals(p.getPeriod())).findFirst().orElseThrow();
        itemService.planDose(target.getId(), "50mg", UUID.randomUUID(), 1L);
        itemService.planDose(sibling.getId(), "25mg", UUID.randomUUID(), 1L);

        var res = restTemplate.exchange(
                "/api/prescriptions/day-parts/{dayPartId}/cancel-assignment", HttpMethod.PUT,
                authEntity(null, getDoctorToken()),
                PrescriptionDayPartResponse.class, target.getId());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getIsPlanned()).isFalse();
        assertThat(res.getBody().getDose()).isNull();

        PrescriptionDayPart cleared = partRepository.findById(target.getId()).orElseThrow();
        assertThat(cleared.getIsPlanned()).isFalse();
        assertThat(cleared.getIsPlannedFinished()).isFalse();
        assertThat(cleared.getDose()).isNull();
        PrescriptionDayPart untouched = partRepository.findById(sibling.getId()).orElseThrow();
        assertThat(untouched.getIsPlanned()).isTrue();
        assertThat(untouched.getDose()).isEqualTo("25mg");
        assertThat(hasAudit("PrescriptionDayPart", target.getId(), "CANCEL_ASSIGNMENT")).isTrue();
    }

    @Test
    void cancelAssignment_onCompleted_returnsUnprocessableEntity() {
        UUID itemId = newItemId();
        PrescriptionDayPart part = morningPartOf(itemId, LocalDate.now());
        itemService.planDose(part.getId(), "50mg", UUID.randomUUID(), 1L);
        itemService.markCompleted(part.getId(), UUID.randomUUID());

        var res = restTemplate.exchange(
                "/api/prescriptions/day-parts/{dayPartId}/cancel-assignment", HttpMethod.PUT,
                authEntity(null, getDoctorToken()),
                String.class, part.getId());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void removeDay_asDoctor_removesAndWritesAudit() {
        UUID itemId = newItemId();
        List<PrescriptionItemDay> days = itemService.getDays(itemId);
        assertThat(days).hasSizeGreaterThanOrEqualTo(2);
        UUID dayId = days.get(days.size() - 1).getId();

        var res = restTemplate.exchange(
                "/api/prescriptions/items/{itemId}/days/{dayId}", HttpMethod.DELETE,
                authEntity(null, getDoctorToken()),
                String.class, itemId, dayId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(dayRepository.findById(dayId).orElseThrow().getDeleted()).isTrue();
        assertThat(hasAudit("PrescriptionItemDay", dayId, "REMOVE")).isTrue();
    }

    @Test
    void removeDay_lastDay_returnsUnprocessableEntity() {
        PrescriptionList list = listService.create(1003L);
        UUID itemId = itemService.addItem(list.getId(), "IT-Single", "IV", "test").getId();
        List<PrescriptionItemDay> days = itemService.getDays(itemId);
        for (PrescriptionItemDay d : days.subList(1, days.size())) {
            itemService.removeDay(itemId, d.getId(), 1L);
        }
        UUID lastDayId = itemService.getDays(itemId).get(0).getId();

        var res = restTemplate.exchange(
                "/api/prescriptions/items/{itemId}/days/{dayId}", HttpMethod.DELETE,
                authEntity(null, getDoctorToken()),
                String.class, itemId, lastDayId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void addDay_asDoctor_appendsUnplannedDay() {
        UUID itemId = newItemId();
        int before = itemService.getDays(itemId).size();

        var res = restTemplate.exchange(
                "/api/prescriptions/items/{itemId}/days", HttpMethod.POST,
                authEntity(null, getDoctorToken()),
                PrescriptionItemResponse.class, itemId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getDayParts()).hasSize(before * 4 + 4);
        List<PrescriptionItemDay> after = itemService.getDays(itemId);
        assertThat(after).hasSize(before + 1);
    }
}
