package com.superhumans.integration;

import com.superhumans.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FluidBalanceIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");

    @Test
    void getFluidBalance_returnsEmptyInitially() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/fluid-balance", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<FluidBalanceResponse>>() {},
                SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void recalculateFluidBalance_withHourlyData_returnsAggregated() {
        HourlyRecordCreateRequest hrReq = new HourlyRecordCreateRequest();
        hrReq.setRecordTime(LocalDateTime.now().withHour(10));
        hrReq.setUrineOutput(500.0);
        hrReq.setDrainOutput(200.0);

        var hrEntity = authEntity(hrReq, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{dayId}/hourly-records", HttpMethod.POST, hrEntity,
                HourlyRecordResponse.class, SEED_DAY_ID);

        var recalcEntity = authGet(getDoctorToken());
        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/fluid-balance/recalculate",
                HttpMethod.POST, recalcEntity,
                new ParameterizedTypeReference<List<FluidBalanceResponse>>() {},
                SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
    void recalculateThenGet_returnsSameData() {
        HourlyRecordCreateRequest hrReq1 = new HourlyRecordCreateRequest();
        hrReq1.setRecordTime(LocalDateTime.now().withHour(14));
        hrReq1.setUrineOutput(300.0);

        MedicalOrderCreateRequest orderReq = new MedicalOrderCreateRequest();
        orderReq.setCategory("INFUSION");
        orderReq.setDrugName("Фізрозчин");
        orderReq.setDose("500");
        orderReq.setUnit("мл");
        orderReq.setRoute("в/в");
        orderReq.setFrequency("крапельно");
        orderReq.setStartTime(LocalDateTime.now().withHour(14));

        var hrEntity = authEntity(hrReq1, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{dayId}/hourly-records", HttpMethod.POST, hrEntity,
                HourlyRecordResponse.class, SEED_DAY_ID);

        var orderEntity = authEntity(orderReq, getDoctorToken());
        var orderRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/orders", HttpMethod.POST, orderEntity,
                MedicalOrderResponse.class, SEED_DAY_ID);

        UUID orderId = orderRes.getBody().getId();
        OrderExecutionCreateRequest execReq = new OrderExecutionCreateRequest(
                UUID.randomUUID(), LocalDateTime.now().withHour(14), "500", "");
        var execEntity = authEntity(execReq, getNurseToken());
        restTemplate.exchange(
                "/api/orders/{orderId}/execute", HttpMethod.POST, execEntity,
                OrderExecutionResponse.class, orderId);

        var recalcEntity = authGet(getDoctorToken());
        restTemplate.exchange(
                "/api/clinical-days/{dayId}/fluid-balance/recalculate",
                HttpMethod.POST, recalcEntity,
                new ParameterizedTypeReference<List<FluidBalanceResponse>>() {},
                SEED_DAY_ID);

        var getEntity = authGet(getDoctorToken());
        var getRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/fluid-balance", HttpMethod.GET, getEntity,
                new ParameterizedTypeReference<List<FluidBalanceResponse>>() {},
                SEED_DAY_ID);

        assertThat(getRes.getBody()).isNotEmpty();
    }

    @Test
    void recalculateFluidBalance_withNoData_returnsEmpty() {
        // Use a fresh episode with no data
        ClinicalDayCreateRequest dayReq = new ClinicalDayCreateRequest(
                UUID.fromString("a2222222-2222-2222-2222-222222222222"),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        var dayEntity = authEntity(dayReq, getDoctorToken());
        var dayRes = restTemplate.exchange(
                "/api/clinical-days", HttpMethod.POST, dayEntity,
                ClinicalDayResponse.class);

        UUID newDayId = dayRes.getBody().getId();

        var recalcEntity = authGet(getDoctorToken());
        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/fluid-balance/recalculate",
                HttpMethod.POST, recalcEntity,
                new ParameterizedTypeReference<List<FluidBalanceResponse>>() {},
                newDayId);

        assertThat(res.getBody()).isEmpty();
    }
}
