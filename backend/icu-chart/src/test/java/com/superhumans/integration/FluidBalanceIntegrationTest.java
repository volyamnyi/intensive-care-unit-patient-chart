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
    private static final UUID OTHER_OPEN_DAY_ID =
            UUID.fromString("b2222222-2222-2222-2222-222222222222");

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
        var futureTime = LocalDateTime.now().plusHours(2);

        HourlyRecordCreateRequest hrReq1 = new HourlyRecordCreateRequest();
        hrReq1.setRecordTime(futureTime);
        hrReq1.setUrineOutput(300.0);

        MedicalOrderCreateRequest orderReq = new MedicalOrderCreateRequest();
        orderReq.setCategory("INFUSION");
        orderReq.setDrugName("Фізрозчин");
        orderReq.setDose("500");
        orderReq.setUnit("мл");
        orderReq.setRoute("в/в");
        orderReq.setFrequency("крапельно");
        orderReq.setStartTime(LocalDateTime.now().withHour(8));

        var hrEntity = authEntity(hrReq1, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{dayId}/hourly-records", HttpMethod.POST, hrEntity,
                HourlyRecordResponse.class, SEED_DAY_ID);

        var orderEntity = authEntity(orderReq, getDoctorToken());
        var orderRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/orders", HttpMethod.POST, orderEntity,
                MedicalOrderResponse.class, SEED_DAY_ID);

        UUID orderId = orderRes.getBody().getId();
        OrderExecutionPlanRequest planReq = new OrderExecutionPlanRequest(13, "500");
        var planEntity = authEntity(planReq, getDoctorToken());
        restTemplate.exchange(
                "/api/orders/{orderId}/plan", HttpMethod.PUT, planEntity,
                OrderExecutionResponse.class, orderId);

        OrderExecutionCreateRequest execReq = new OrderExecutionCreateRequest(13, "500", "");
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
    void autoRecalculate_afterHourlyRecord_returnsPopulated() {
        HourlyRecordCreateRequest hrReq = new HourlyRecordCreateRequest();
        hrReq.setRecordTime(LocalDateTime.now().withHour(8));
        hrReq.setUrineOutput(400.0);

        var hrEntity = authEntity(hrReq, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{dayId}/hourly-records", HttpMethod.POST, hrEntity,
                HourlyRecordResponse.class, OTHER_OPEN_DAY_ID);

        var getEntity = authGet(getDoctorToken());
        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/fluid-balance", HttpMethod.GET, getEntity,
                new ParameterizedTypeReference<List<FluidBalanceResponse>>() {},
                OTHER_OPEN_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
        assertThat(res.getBody().get(0).getOutput()).isPositive();
    }

    @Test
    void autoRecalculate_afterOrderExecution_includesIntake() {
        LocalDateTime startTime = LocalDateTime.now().plusHours(1)
                .withMinute(0).withSecond(0).withNano(0);
        int hour = startTime.getHour();

        MedicalOrderCreateRequest orderReq = new MedicalOrderCreateRequest();
        orderReq.setCategory("INFUSION");
        orderReq.setDrugName("Фізрозчин");
        orderReq.setDose("500");
        orderReq.setUnit("мл");
        orderReq.setRoute("в/в");
        orderReq.setFrequency("крапельно");
        orderReq.setStartTime(startTime);

        var orderEntity = authEntity(orderReq, getDoctorToken());
        var orderRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/orders", HttpMethod.POST, orderEntity,
                MedicalOrderResponse.class, OTHER_OPEN_DAY_ID);

        UUID orderId = orderRes.getBody().getId();
        OrderExecutionPlanRequest planReq = new OrderExecutionPlanRequest(hour, "500");
        var planEntity = authEntity(planReq, getDoctorToken());
        restTemplate.exchange(
                "/api/orders/{orderId}/plan", HttpMethod.PUT, planEntity,
                OrderExecutionResponse.class, orderId);

        OrderExecutionCreateRequest execReq = new OrderExecutionCreateRequest(hour, "500", "");
        var execEntity = authEntity(execReq, getNurseToken());
        restTemplate.exchange(
                "/api/orders/{orderId}/execute", HttpMethod.POST, execEntity,
                OrderExecutionResponse.class, orderId);

        var getEntity = authGet(getDoctorToken());
        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/fluid-balance", HttpMethod.GET, getEntity,
                new ParameterizedTypeReference<List<FluidBalanceResponse>>() {},
                OTHER_OPEN_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
        assertThat(res.getBody()).anyMatch(fb -> fb.getIntake() > 0);
    }

    @Test
    void recalculateFluidBalance_withNoData_returnsEmpty() {
        // Use an existing clinical day that has no hourly records or orders
        UUID freshDayId = UUID.fromString("b3333333-3333-3333-3333-333333333333");

        var recalcEntity = authGet(getDoctorToken());
        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/fluid-balance/recalculate",
                HttpMethod.POST, recalcEntity,
                new ParameterizedTypeReference<List<FluidBalanceResponse>>() {},
                freshDayId);

        assertThat(res.getBody()).isEmpty();
    }
}
