package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsOrderMapper;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Prosthetics order registry. The order document itself is owned by MIS —
 * see {@code spiDocumentProsthesCheck} response field {@code documentUrl}
 * surfaced via {@code GET /api/prosthesis-manufacturing/orders/{id}/document-url}
 * (added in #257). The legacy on-demand local PDF generator
 * ({@code ProstheticsPdfService.generateOrderRecipe}) and the
 * {@code recipe_pdf_data} column have been removed: ICU Chart is a read-only
 * client of MIS and must not synthesise order documents locally.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProstheticsOrderService {

    ProstheticsOrderRepository orderRepository;
    ProstheticsOrderMapper orderMapper;

    @Transactional(readOnly = true)
    public List<ProstheticsOrderResponse> list(String patientId, String status) {
        List<ProstheticsOrder> orders;
        if (patientId != null && status != null) {
            orders = orderRepository.findByPatientIdAndStatus(patientId, OrderStatus.valueOf(status));
        } else if (patientId != null) {
            orders = orderRepository.findByPatientId(patientId);
        } else if (status != null) {
            orders = orderRepository.findByStatus(OrderStatus.valueOf(status));
        } else {
            orders = orderRepository.findAll();
        }
        return orders.stream().map(orderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProstheticsOrderResponse get(UUID id) {
        return orderMapper.toResponse(load(id));
    }

    private ProstheticsOrder load(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }
}
