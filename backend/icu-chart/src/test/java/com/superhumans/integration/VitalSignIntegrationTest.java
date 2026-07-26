package com.superhumans.integration;

import com.superhumans.medicationsheet.dto.*;
import com.superhumans.medicationsheet.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VitalSignIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private VitalSignListRepository vitalListRepository;
    @Autowired
    private VitalSignDayRepository vitalDayRepository;
    @Autowired
    private VitalSignEntryRepository vitalEntryRepository;

    private static final UUID PRESCRIPTION_LIST_ID =
            UUID.fromString("cccc0001-0001-0001-0001-000000000001");
    private static final UUID VITAL_LIST_ID =
            UUID.fromString("bbbb0001-0001-0001-0001-000000000001");
    private static final UUID VITAL_DAY_ID =
            UUID.fromString("bbbb0002-0002-0002-0002-000000000001");
    private static final UUID MORNING_ENTRY_ID =
            UUID.fromString("bbbb0003-0003-0003-0003-000000000001");
    private static final UUID EVENING_ENTRY_ID =
            UUID.fromString("bbbb0004-0004-0004-0004-000000000001");

    @Test
    void getDays_returnsSeedDays() {
        var res = restTemplate.exchange(
                "/api/vital-signs?prescriptionListId={listId}", HttpMethod.GET,
                authGet(getDoctorToken()),
                new ParameterizedTypeReference<List<VitalSignDayResponse>>() {},
                PRESCRIPTION_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull().isNotEmpty();
    }

    @Test
    void getEntries_returnsSeedEntries() {
        var res = restTemplate.exchange(
                "/api/vital-signs/days/{dayId}/entries", HttpMethod.GET,
                authGet(getDoctorToken()),
                new ParameterizedTypeReference<List<VitalSignEntryResponse>>() {},
                VITAL_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull().hasSize(2);
        assertThat(res.getBody().get(0).getTemperature()).isEqualTo(36.6);
        assertThat(res.getBody().get(0).getPulse()).isEqualTo(72);
    }

    @Test
    void createEntry_asNurse_returnsOk() {
        VitalSignEntryRequest req = new VitalSignEntryRequest();
        req.setPrescriptionListId(PRESCRIPTION_LIST_ID.toString());
        req.setTemperature(37.0);
        req.setSystolicBp(125);
        req.setDiastolicBp(82);
        req.setSpo2(97);
        req.setPulse(76);

        var res = restTemplate.exchange(
                "/api/vital-signs", HttpMethod.POST,
                authEntity(req, getNurseToken()),
                VitalSignEntryResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getTemperature()).isEqualTo(37.0);
    }

    @Test
    void createEntry_fillsEmptyEveningSlot() {
        VitalSignEntryRequest req = new VitalSignEntryRequest();
        req.setPrescriptionListId(PRESCRIPTION_LIST_ID.toString());
        req.setTemperature(36.8);
        req.setSystolicBp(118);
        req.setDiastolicBp(78);
        req.setSpo2(99);
        req.setPulse(68);

        var res = restTemplate.exchange(
                "/api/vital-signs", HttpMethod.POST,
                authEntity(req, getNurseToken()),
                VitalSignEntryResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getTemperature()).isEqualTo(36.8);
    }
}
