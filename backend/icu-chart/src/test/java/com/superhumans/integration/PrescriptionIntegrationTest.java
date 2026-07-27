package com.superhumans.integration;

import com.superhumans.medicationsheet.dto.*;
import com.superhumans.medicationsheet.entity.*;
import com.superhumans.medicationsheet.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

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
        assertThat(res.getBody().get(0).getId()).isEqualTo(SEED_LIST_ID.toString());
    }

    @Test
    void getById_returnsSeedPrescription() {
        var res = restTemplate.exchange(
                "/api/prescriptions/{id}", HttpMethod.GET,
                authGet(getDoctorToken()),
                PrescriptionListResponse.class, SEED_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getId()).isEqualTo(SEED_LIST_ID.toString());
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
        execReq.setRequires2pAuth(false);

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
        req.setRequires2pAuth(false);

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
}
