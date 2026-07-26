package com.superhumans.integration;

import com.superhumans.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MedicalOrderIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");

    @Test
    void createOrder_asDoctor_succeeds() {
        MedicalOrderCreateRequest req = new MedicalOrderCreateRequest();
        req.setCategory("MEDICATION");
        req.setDrugName("Норадреналін");
        req.setDose("4");
        req.setUnit("мг");
        req.setRoute("в/в");
        req.setFrequency("інфузія");
        req.setStartTime(LocalDateTime.now());

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/orders", HttpMethod.POST, entity,
                MedicalOrderResponse.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getDrugName()).isEqualTo("Норадреналін");
        assertThat(res.getBody().getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void createOrder_asNurse_returnsForbidden() {
        MedicalOrderCreateRequest req = new MedicalOrderCreateRequest();
        req.setCategory("MEDICATION");
        req.setDrugName("Test");
        req.setDose("5");
        req.setUnit("mg");
        req.setRoute("IV");
        req.setFrequency("BID");
        req.setStartTime(LocalDateTime.now());

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/orders", HttpMethod.POST, entity,
                String.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getOrders_returnsCreatedOrders() {
        MedicalOrderCreateRequest req = new MedicalOrderCreateRequest();
        req.setCategory("MEDICATION");
        req.setDrugName("Тест-препарат");
        req.setDose("10");
        req.setUnit("мл");
        req.setRoute("в/в");
        req.setFrequency("1 раз");
        req.setStartTime(LocalDateTime.now());

        var createEntity = authEntity(req, getDoctorToken());
        restTemplate.exchange(
                "/api/clinical-days/{dayId}/orders", HttpMethod.POST, createEntity,
                MedicalOrderResponse.class, SEED_DAY_ID);

        var listEntity = authGet(getDoctorToken());
        var listRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/orders", HttpMethod.GET, listEntity,
                new ParameterizedTypeReference<List<MedicalOrderResponse>>() {},
                SEED_DAY_ID);

        assertThat(listRes.getBody()).isNotEmpty();
    }

    @Test
    void updateOrder_updatesFields() {
        MedicalOrderCreateRequest createReq = new MedicalOrderCreateRequest();
        createReq.setCategory("MEDICATION");
        createReq.setDrugName("Допамин");
        createReq.setDose("5");
        createReq.setUnit("мкг");
        createReq.setRoute("в/в");
        createReq.setFrequency("інфузія");
        createReq.setStartTime(LocalDateTime.now());

        var createEntity = authEntity(createReq, getDoctorToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/orders", HttpMethod.POST, createEntity,
                MedicalOrderResponse.class, SEED_DAY_ID);

        UUID orderId = createRes.getBody().getId();

        MedicalOrderPatchRequest patchReq = new MedicalOrderPatchRequest(
                "10", null, null, null, 0);
        var patchEntity = authEntity(patchReq, getDoctorToken());
        var patchRes = restTemplate.exchange(
                "/api/orders/{id}", HttpMethod.PATCH, patchEntity,
                Void.class, orderId);

        assertThat(patchRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void cancelOrder_cancelsSuccessfully() {
        MedicalOrderCreateRequest createReq = new MedicalOrderCreateRequest();
        createReq.setCategory("MEDICATION");
        createReq.setDrugName("Фентаніл");
        createReq.setDose("100");
        createReq.setUnit("мкг");
        createReq.setRoute("в/в");
        createReq.setFrequency("болюсно");
        createReq.setStartTime(LocalDateTime.now());

        var createEntity = authEntity(createReq, getDoctorToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/orders", HttpMethod.POST, createEntity,
                MedicalOrderResponse.class, SEED_DAY_ID);

        UUID orderId = createRes.getBody().getId();

        MedicalOrderPatchRequest cancelReq = new MedicalOrderPatchRequest(
                null, null, null, null, 0);
        var cancelEntity = authEntity(cancelReq, getDoctorToken());
        var cancelRes = restTemplate.exchange(
                "/api/orders/{id}/cancel", HttpMethod.POST, cancelEntity,
                Void.class, orderId);

        assertThat(cancelRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void executeOrder_executesSuccessfully() {
        MedicalOrderCreateRequest createReq = new MedicalOrderCreateRequest();
        createReq.setCategory("MEDICATION");
        createReq.setDrugName("Гепарин");
        createReq.setDose("5000");
        createReq.setUnit("ОД");
        createReq.setRoute("п/ш");
        createReq.setFrequency("2 рази");
        createReq.setStartTime(LocalDateTime.now());

        var createEntity = authEntity(createReq, getDoctorToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/orders", HttpMethod.POST, createEntity,
                MedicalOrderResponse.class, SEED_DAY_ID);

        UUID orderId = createRes.getBody().getId();

        OrderExecutionCreateRequest execReq = new OrderExecutionCreateRequest(
                13L, LocalDateTime.now(), "5000", "Виконано");
        var execEntity = authEntity(execReq, getNurseToken());

        var execRes = restTemplate.exchange(
                "/api/orders/{orderId}/execute", HttpMethod.POST, execEntity,
                OrderExecutionResponse.class, orderId);

        assertThat(execRes.getBody().getActualDose()).isEqualTo("5000");
    }

    @Test
    void getExecutions_returnsExecutionsList() {
        MedicalOrderCreateRequest createReq = new MedicalOrderCreateRequest();
        createReq.setCategory("MEDICATION");
        createReq.setDrugName("Магній");
        createReq.setDose("25%");
        createReq.setUnit("10 мл");
        createReq.setRoute("в/в");
        createReq.setFrequency("1 раз");
        createReq.setStartTime(LocalDateTime.now());

        var createEntity = authEntity(createReq, getDoctorToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/orders", HttpMethod.POST, createEntity,
                MedicalOrderResponse.class, SEED_DAY_ID);

        UUID orderId = createRes.getBody().getId();

        OrderExecutionCreateRequest execReq = new OrderExecutionCreateRequest(
                13L, LocalDateTime.now(), "10 мл", "");
        var execEntity = authEntity(execReq, getNurseToken());
        restTemplate.exchange(
                "/api/orders/{orderId}/execute", HttpMethod.POST, execEntity,
                OrderExecutionResponse.class, orderId);

        var listEntity = authGet(getDoctorToken());
        var listRes = restTemplate.exchange(
                "/api/orders/{orderId}/executions", HttpMethod.GET, listEntity,
                new ParameterizedTypeReference<List<OrderExecutionResponse>>() {},
                orderId);

        assertThat(listRes.getBody()).isNotEmpty();
    }
}
