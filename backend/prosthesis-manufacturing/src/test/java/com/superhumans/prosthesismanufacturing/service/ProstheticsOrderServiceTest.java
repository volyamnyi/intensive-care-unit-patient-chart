package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsOrderMapper;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProstheticsOrderServiceTest {

    @Mock
    private ProstheticsOrderRepository orderRepository;
    @Mock
    private ProstheticsOrderMapper orderMapper;

    @InjectMocks
    private ProstheticsOrderService orderService;

    private UUID orderId;
    private String patientId;
    private ProstheticsPatient patient;
    private ProstheticsOrder order;
    private ProstheticsOrderResponse response;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        patientId = "900001";
        patient = ProstheticsPatient.builder().build();
        patient.setId(patientId);
        order = ProstheticsOrder.builder()
                .orderNumber("PR-2026-0001")
                .status(OrderStatus.IN_PROGRESS)
                .patient(patient)
                .build();
        order.setId(orderId);
        response = ProstheticsOrderResponse.builder()
                .id(orderId)
                .orderNumber("PR-2026-0001")
                .status(OrderStatus.IN_PROGRESS.name())
                .build();
    }

    @Test
    void list_patientAndStatus_delegatesToFilteredQuery() {
        when(orderRepository.findByPatientIdAndStatus("900001", OrderStatus.IN_PROGRESS))
                .thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        var res = orderService.list("900001", "IN_PROGRESS");

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getId()).isEqualTo(orderId);
    }

    @Test
    void list_patientOnly_delegatesToPatientQuery() {
        when(orderRepository.findByPatientId("900001")).thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        var res = orderService.list("900001", null);

        assertThat(res).hasSize(1);
        verify(orderRepository, never()).findByStatus(any());
        verify(orderRepository, never()).findAll();
    }

    @Test
    void list_statusOnly_delegatesToStatusQuery() {
        when(orderRepository.findByStatus(OrderStatus.NEW)).thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        var res = orderService.list(null, "NEW");

        assertThat(res).hasSize(1);
        verify(orderRepository, never()).findByPatientId(any());
        verify(orderRepository, never()).findAll();
    }

    @Test
    void list_noFilters_returnsAll() {
        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        var res = orderService.list(null, null);

        assertThat(res).hasSize(1);
    }

    @Test
    void list_invalidStatus_throwsWithoutQuerying() {
        assertThatThrownBy(() -> orderService.list(null, "NOPE"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never()).findAll();
        verify(orderRepository, never()).findByPatientId(any());
        verify(orderRepository, never()).findByStatus(any());
    }

    @Test
    void get_whenFound_returnsMappedResponse() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(response);

        var res = orderService.get(orderId);

        assertThat(res.getId()).isEqualTo(orderId);
        assertThat(res.getOrderNumber()).isEqualTo("PR-2026-0001");
    }

    @Test
    void get_whenNotFound_throws() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(orderId))
                .isInstanceOf(NotFoundException.class);
    }
}
