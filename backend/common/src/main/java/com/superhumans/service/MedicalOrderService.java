package com.superhumans.service;

import com.superhumans.dto.MedicalOrderCreateRequest;
import com.superhumans.dto.MedicalOrderPatchRequest;
import com.superhumans.dto.MedicalOrderResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.MedicalOrder;
import com.superhumans.entity.MedicalOrderStatus;
import com.superhumans.exception.BusinessException;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.ErrorCode;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.MedicalOrderMapper;
import com.superhumans.repository.icu.ClinicalDayRepository;
import com.superhumans.repository.icu.MedicalOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalOrderService {

    MedicalOrderRepository medicalOrderRepository;
    ClinicalDayRepository clinicalDayRepository;
    AuditService auditService;
    MedicalOrderMapper medicalOrderMapper;

    public MedicalOrderResponse getOrder(UUID id) {
        MedicalOrder order = medicalOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical order not found: " + id));
        return medicalOrderMapper.toResponse(order);
    }

    public List<MedicalOrderResponse> getOrdersByClinicalDay(UUID clinicalDayId) {
        return medicalOrderRepository.findByClinicalDayIdOrderByStartTimeAsc(clinicalDayId)
                .stream().map(medicalOrderMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public MedicalOrderResponse createOrder(UUID clinicalDayId, MedicalOrderCreateRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + clinicalDayId));
        assertNotLocked(day);

        MedicalOrder order = medicalOrderMapper.toEntity(request);
        order.setClinicalDay(day);
        order.setStatus(MedicalOrderStatus.ACTIVE);
        order.setCreatedBy(userId);
        order.setUpdatedBy(userId);

        if (order.getStartTime() != null) {
            LocalDateTime rounded = roundToNextHour(order.getStartTime());
            if (rounded.isBefore(LocalDateTime.now().minusHours(1))) {
                throw new BusinessException(ErrorCode.PAST_HOUR_ORDER,
                        "Не можна створити призначення на минулу годину");
            }
            order.setStartTime(rounded);
        }

        order = medicalOrderRepository.save(order);
        auditService.logCreate("MedicalOrder", order.getId(), userId);
        return medicalOrderMapper.toResponse(order);
    }

    @Transactional
    public MedicalOrderResponse updateOrder(UUID id, MedicalOrderPatchRequest request, Long userId) {
        MedicalOrder order = medicalOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical order not found: " + id));

        if (!order.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Medical order was modified by another user");
        }
        assertNotLocked(order.getClinicalDay());

        if (request.getDose() != null) order.setDose(request.getDose());
        if (request.getRoute() != null) order.setRoute(request.getRoute());
        if (request.getFrequency() != null) order.setFrequency(request.getFrequency());
        if (request.getEndTime() != null) order.setEndTime(request.getEndTime());
        order.setUpdatedBy(userId);
        order = medicalOrderRepository.save(order);
        auditService.logUpdate("MedicalOrder", id, userId, null, "Updated order fields");
        return medicalOrderMapper.toResponse(order);
    }

    @Transactional
    public MedicalOrderResponse cancelOrder(UUID id, Integer version, Long userId) {
        MedicalOrder order = medicalOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Medical order not found: " + id));

        if (!order.getVersion().equals(version)) {
            throw new VersionConflictException("Medical order was modified by another user");
        }

        if (order.getStatus() == MedicalOrderStatus.CANCELLED) {
            throw new DocumentLockedException("Medical order is already cancelled");
        }
        if (order.getStatus() == MedicalOrderStatus.COMPLETED) {
            throw new DocumentLockedException("Medical order is already completed");
        }

        order.setStatus(MedicalOrderStatus.CANCELLED);
        order.setUpdatedBy(userId);
        order = medicalOrderRepository.save(order);
        auditService.logAction("MedicalOrder", id, "CANCEL", userId);
        return medicalOrderMapper.toResponse(order);
    }

    private LocalDateTime roundToNextHour(LocalDateTime dt) {
        if (dt.getMinute() > 0 || dt.getSecond() > 0 || dt.getNano() > 0) {
            return dt.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        }
        return dt;
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.NURSE_SIGNED
                || day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }
}
