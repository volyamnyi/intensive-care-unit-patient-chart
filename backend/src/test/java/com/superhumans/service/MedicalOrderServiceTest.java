package com.superhumans.service;

import com.superhumans.dto.MedicalOrderCreateRequest;
import com.superhumans.dto.MedicalOrderPatchRequest;
import com.superhumans.dto.MedicalOrderResponse;
import com.superhumans.entity.*;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.MedicalOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalOrderServiceTest {

    @Mock
    private MedicalOrderRepository medicalOrderRepository;

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private MedicalOrderService medicalOrderService;

    @Captor
    private ArgumentCaptor<MedicalOrder> orderCaptor;

    private UUID orderId;
    private UUID clinicalDayId;
    private UUID userId;
    private ClinicalDay clinicalDay;
    private MedicalOrder testOrder;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        clinicalDayId = UUID.randomUUID();
        userId = UUID.randomUUID();
        clinicalDay = ClinicalDay.builder()
                .status(ClinicalDayStatus.OPEN)
                .build();
        clinicalDay.setId(clinicalDayId);
        clinicalDay.setVersion(0);
        testOrder = MedicalOrder.builder()
                .drugName("Test Drug")
                .dose("10")
                .unit("mg")
                .status(MedicalOrderStatus.ACTIVE)
                .build();
        testOrder.setId(orderId);
        testOrder.setClinicalDay(clinicalDay);
        testOrder.setVersion(0);
    }

    @Test
    void getOrder_whenFound_returnsResponse() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        MedicalOrderResponse res = medicalOrderService.getOrder(orderId);

        assertThat(res.getId()).isEqualTo(orderId);
        assertThat(res.getDrugName()).isEqualTo("Test Drug");
    }

    @Test
    void getOrder_whenNotFound_throws() {
        when(medicalOrderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalOrderService.getOrder(orderId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOrdersByClinicalDay_returnsList() {
        when(medicalOrderRepository.findByClinicalDayIdOrderByStartTimeAsc(clinicalDayId))
                .thenReturn(List.of(testOrder));

        var results = medicalOrderService.getOrdersByClinicalDay(clinicalDayId);

        assertThat(results).hasSize(1);
    }

    @Test
    void createOrder_setsActiveStatus() {
        MedicalOrderCreateRequest req = new MedicalOrderCreateRequest();
        req.setCategory("MEDICATION");
        req.setDrugName("Drug");
        req.setDose("5");
        req.setUnit("mg");
        req.setRoute("IV");
        req.setFrequency("BID");
        req.setStartTime(LocalDateTime.now());

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        MedicalOrder saved = MedicalOrder.builder().status(MedicalOrderStatus.ACTIVE).build();
        saved.setId(orderId);
        saved.setClinicalDay(clinicalDay);
        when(medicalOrderRepository.save(any(MedicalOrder.class))).thenReturn(saved);

        MedicalOrderResponse res = medicalOrderService.createOrder(clinicalDayId, req, userId);

        verify(medicalOrderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(MedicalOrderStatus.ACTIVE);
        verify(auditService).logCreate("MedicalOrder", orderId, userId);
    }

    @Test
    void createOrder_whenDaySigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        MedicalOrderCreateRequest req = new MedicalOrderCreateRequest();
        req.setCategory("MEDICATION");
        req.setDrugName("Drug");
        req.setDose("5");
        req.setUnit("mg");
        req.setRoute("IV");
        req.setFrequency("BID");
        req.setStartTime(LocalDateTime.now());

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));

        assertThatThrownBy(() -> medicalOrderService.createOrder(clinicalDayId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void updateOrder_withVersionMismatch_throws() {
        MedicalOrderPatchRequest req = new MedicalOrderPatchRequest(null, null, null, null, 999);
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> medicalOrderService.updateOrder(orderId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void updateOrder_updatesFields() {
        MedicalOrderPatchRequest req = new MedicalOrderPatchRequest("20", "PO", "TID", null, 0);

        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        MedicalOrder saved = MedicalOrder.builder().build();
        saved.setId(orderId);
        saved.setClinicalDay(clinicalDay);
        saved.setVersion(1);
        when(medicalOrderRepository.save(any(MedicalOrder.class))).thenReturn(saved);

        MedicalOrderResponse res = medicalOrderService.updateOrder(orderId, req, userId);

        verify(medicalOrderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getDose()).isEqualTo("20");
        assertThat(orderCaptor.getValue().getRoute()).isEqualTo("PO");
        assertThat(orderCaptor.getValue().getFrequency()).isEqualTo("TID");
        verify(auditService).logUpdate("MedicalOrder", orderId, userId, null, "Updated order fields");
    }

    @Test
    void cancelOrder_cancelsSuccessfully() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        MedicalOrder saved = MedicalOrder.builder().status(MedicalOrderStatus.CANCELLED).build();
        saved.setId(orderId);
        saved.setClinicalDay(clinicalDay);
        when(medicalOrderRepository.save(any(MedicalOrder.class))).thenReturn(saved);

        MedicalOrderResponse res = medicalOrderService.cancelOrder(orderId, 0, userId);

        verify(medicalOrderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(MedicalOrderStatus.CANCELLED);
        verify(auditService).logAction("MedicalOrder", orderId, "CANCEL", userId);
    }

    @Test
    void cancelOrder_whenAlreadyCancelled_throws() {
        testOrder.setStatus(MedicalOrderStatus.CANCELLED);
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> medicalOrderService.cancelOrder(orderId, 0, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void cancelOrder_whenCompleted_throws() {
        testOrder.setStatus(MedicalOrderStatus.COMPLETED);
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> medicalOrderService.cancelOrder(orderId, 0, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void cancelOrder_withVersionMismatch_throws() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> medicalOrderService.cancelOrder(orderId, 999, userId))
                .isInstanceOf(VersionConflictException.class);
    }
}
