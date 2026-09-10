package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsOrderMapper;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProstheticsOrderService {

    ProstheticsOrderRepository orderRepository;
    ProstheticsOrderMapper orderMapper;
    MisService misService;
    DocumentUrlAvailability documentUrlAvailability;

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

    /**
     * Returns every limb-prosthesis order document MIS holds for the patient
     * ({@code spiDocumentProsthesCheck} with {@code PatientID}, templates
     * 120 «upper limbs» / 121 «lower limbs»). Powers setup step 2
     * (order selection): the returned {@code documentUrl} values feed step 3
     * (review), which embeds them directly.
     * <p>
     * A document is dropped when it has no URL or its URL answers HTTP 404
     * (verified per document via {@link DocumentUrlAvailability}); any other
     * probe outcome keeps the document (fail-open). MIS order is preserved.
     *
     * @param patientId digits-only MIS patient id
     * @throws NotFoundException when {@code patientId} is not numeric
     */
    @Transactional(readOnly = true)
    public List<DocumentMisDTO> getAllLowerLimbsOrdersForByPatientId(String patientId) {
        Long numericPatientId = parsePatientId(patientId);
        List<DocumentMisDTO> documents = misService.getPatientDocuments(numericPatientId);
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .filter(d -> d.getDocumentTemplateId() != null
                        && ProstheticsEligibilityService.ELIGIBLE_DOCUMENT_TEMPLATE_IDS
                                .contains(d.getDocumentTemplateId()))
                .filter(d -> d.getDocumentUrl() != null && !d.getDocumentUrl().isBlank())
                .filter(d -> documentUrlAvailability.isAvailable(d.getDocumentUrl()))
                .toList();
    }

    private static Long parsePatientId(String patientId) {
        if (patientId == null || !patientId.chars().allMatch(Character::isDigit)) {
            throw new NotFoundException("Patient id is not numeric: " + patientId);
        }
        try {
            return Long.valueOf(patientId);
        } catch (NumberFormatException ex) {
            throw new NotFoundException("Patient id is not numeric: " + patientId);
        }
    }

    private ProstheticsOrder load(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }
}
