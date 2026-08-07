package com.superhumans.prosthesismanufacturing.mapper;

import com.superhumans.prosthesismanufacturing.dto.FlowInstanceResponse;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsPatientResponse;
import com.superhumans.prosthesismanufacturing.dto.StepExecutionResponse;
import com.superhumans.prosthesismanufacturing.entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InstanceMapperTest {

    private final FlowInstanceMapper instanceMapper = new FlowInstanceMapperImpl();
    private final ProstheticsPatientMapper patientMapper = new ProstheticsPatientMapperImpl();
    private final ProstheticsOrderMapper orderMapper = new ProstheticsOrderMapperImpl();

    @Test
    void shouldMapInstanceToResponse() {
        UUID instanceId = UUID.randomUUID();
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .patientId("90001")
                .orderId(UUID.randomUUID())
                .assignedUserId(21L)
                .status(FlowInstanceStatus.IN_PROGRESS)
                .currentStageId(UUID.randomUUID())
                .currentStepId(UUID.randomUUID())
                .startTime(LocalDateTime.of(2026, 8, 4, 9, 0))
                .totalActiveSeconds(1200L)
                .totalIdleSeconds(300L)
                .reworkCount(1)
                .build();
        instance.setId(instanceId);

        FlowInstanceResponse response = instanceMapper.toResponse(instance);

        assertThat(response.getId()).isEqualTo(instanceId);
        assertThat(response.getAssignedUserId()).isEqualTo(21L);
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getTotalActiveSeconds()).isEqualTo(1200L);
        assertThat(response.getTotalIdleSeconds()).isEqualTo(300L);
        assertThat(response.getReworkCount()).isEqualTo(1);
        assertThat(response.getStartTime()).isEqualTo(instance.getStartTime());
    }

    @Test
    void shouldMapExecutionToResponse() {
        UUID instanceId = UUID.randomUUID();
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .build();
        instance.setId(instanceId);

        StepExecution execution = StepExecution.builder()
                .instance(instance)
                .stageId(UUID.randomUUID())
                .stepId(UUID.randomUUID())
                .attemptNumber(2)
                .status(StepExecutionStatus.REWORK)
                .activeSeconds(600L)
                .values("{\"element1\":\"42\"}")
                .completedBy(21L)
                .build();
        execution.setId(UUID.randomUUID());

        StepExecutionResponse response = instanceMapper.toExecutionResponse(execution);

        assertThat(response.getInstanceId()).isEqualTo(instanceId);
        assertThat(response.getAttemptNumber()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo("REWORK");
        assertThat(response.getValues()).isEqualTo("{\"element1\":\"42\"}");
        assertThat(response.getCompletedBy()).isEqualTo(21L);
    }

    @Test
    void shouldMapPatientToResponse() {
        ProstheticsPatient patient = ProstheticsPatient.builder()
                .pib("Сніжко Оксана Володимирівна")
                .birthDate(LocalDate.of(1978, 5, 12))
                .gender("female")
                .heightCm(168)
                .weightKg(72)
                .socialStatus("працююча")
                .cause("травма")
                .amputationLevel("передпліччя")
                .stump("[{\"label\":\"19 см\",\"value\":\"19\"}]")
                .build();
        patient.setId("90001");

        ProstheticsPatientResponse response = patientMapper.toResponse(patient);

        assertThat(response.getPib()).isEqualTo("Сніжко Оксана Володимирівна");
        assertThat(response.getBirthDate()).isEqualTo(LocalDate.of(1978, 5, 12));
        assertThat(response.getHeightCm()).isEqualTo(168);
        assertThat(response.getStump()).isEqualTo("[{\"label\":\"19 см\",\"value\":\"19\"}]");
    }

    @Test
    void shouldMapOrderWithRecipePdf() {
        ProstheticsPatient patient = ProstheticsPatient.builder()
                .pib("Сніжко Оксана Володимирівна")
                .build();
        patient.setId("90001");

        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("ПВ-26-0413")
                .patient(patient)
                .prosthesisType("протез передпліччя")
                .productType(ProductType.UPPER_LIMB)
                .limbSide(LimbSide.RIGHT)
                .doctorName("Бондаренко І.П.")
                .prescriptionDate(LocalDate.of(2026, 7, 10))
                .status(OrderStatus.NEW)
                .recipePdfData(new byte[]{1, 2, 3})
                .build();
        order.setId(UUID.randomUUID());

        ProstheticsOrderResponse response = orderMapper.toResponse(order);

        assertThat(response.getOrderNumber()).isEqualTo("ПВ-26-0413");
        assertThat(response.getPatientId()).isEqualTo(patient.getId());
        assertThat(response.getProductType()).isEqualTo("UPPER_LIMB");
        assertThat(response.getLimbSide()).isEqualTo("RIGHT");
        assertThat(response.getStatus()).isEqualTo("NEW");
        assertThat(response.getHasRecipePdf()).isTrue();
    }

    @Test
    void shouldMapOrderWithoutRecipePdf() {
        ProstheticsPatient patient = ProstheticsPatient.builder()
                .pib("Гаврилюк Тарас Олексійович")
                .build();
        patient.setId("90001");

        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("ПВ-26-0414")
                .patient(patient)
                .status(OrderStatus.NEW)
                .build();
        order.setId(UUID.randomUUID());

        ProstheticsOrderResponse response = orderMapper.toResponse(order);

        assertThat(response.getHasRecipePdf()).isFalse();
    }
}
