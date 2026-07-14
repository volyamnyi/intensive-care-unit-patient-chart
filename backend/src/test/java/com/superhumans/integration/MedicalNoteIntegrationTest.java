package com.superhumans.integration;

import com.superhumans.dto.MedicalNoteCreateRequest;
import com.superhumans.dto.MedicalNoteResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalNoteIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID NURSE_SIGNED_DAY_ID =
            UUID.fromString("b1111112-1111-1111-1111-111111111111");

    @Test
    void getNotes_returnsEmptyListInitially() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/notes", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<MedicalNoteResponse>>() {},
                SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void createNote_asDoctor_succeeds() {
        MedicalNoteCreateRequest req = new MedicalNoteCreateRequest();
        req.setNoteType("CLINICAL");
        req.setText("Пацієнт стабільний, гемодинаміка в нормі.");

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/notes", HttpMethod.POST, entity,
                MedicalNoteResponse.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getNoteType()).isEqualTo("CLINICAL");
        assertThat(res.getBody().getText()).isEqualTo("Пацієнт стабільний, гемодинаміка в нормі.");
        assertThat(res.getBody().getClinicalDayId()).isEqualTo(SEED_DAY_ID);
        assertThat(res.getBody().getAuthorId()).isNotNull();
    }

    @Test
    void createNote_asNurse_succeeds() {
        MedicalNoteCreateRequest req = new MedicalNoteCreateRequest();
        req.setNoteType("OBSERVATION");
        req.setText("Температура 37.2, сатурація 98%.");

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/notes", HttpMethod.POST, entity,
                MedicalNoteResponse.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getNoteType()).isEqualTo("OBSERVATION");
        assertThat(res.getBody().getRole()).isEqualTo("NURSE");
    }

    @Test
    void createNote_onNurseSignedDay_succeeds() {
        MedicalNoteCreateRequest req = new MedicalNoteCreateRequest();
        req.setNoteType("CLINICAL");
        req.setText("Нотатка на підписаному дні.");

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/notes", HttpMethod.POST, entity,
                MedicalNoteResponse.class, NURSE_SIGNED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
    }

    @Test
    void createThenListNotes_returnsCreatedNote() {
        MedicalNoteCreateRequest req = new MedicalNoteCreateRequest();
        req.setNoteType("CLINICAL");
        req.setText("План лікування: антибіотикотерапія.");

        var createEntity = authEntity(req, getDoctorToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/notes", HttpMethod.POST, createEntity,
                MedicalNoteResponse.class, SEED_DAY_ID);

        UUID newId = createRes.getBody().getId();

        var listEntity = authGet(getDoctorToken());
        var listRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/notes", HttpMethod.GET, listEntity,
                new ParameterizedTypeReference<List<MedicalNoteResponse>>() {},
                SEED_DAY_ID);

        assertThat(listRes.getBody()).isNotEmpty();
        assertThat(listRes.getBody()).anyMatch(n -> n.getId().equals(newId));
    }
}
