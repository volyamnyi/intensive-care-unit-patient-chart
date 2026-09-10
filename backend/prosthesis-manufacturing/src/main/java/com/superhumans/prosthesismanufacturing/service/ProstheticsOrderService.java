package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.prosthesismanufacturing.dto.OrderProvisionRequest;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsOrderMapper;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
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
    ProstheticsPatientRepository patientRepository;

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

    /**
     * Provisions the local order for an MIS limb-prosthesis document
     * (setup step 2 → execution chain). Find-or-create by the deterministic
     * number {@code MIS-{patientId}-{documentId}}: picking the same MIS
     * document twice returns the same local order, never a duplicate.
     * The local patient row is created on demand from MIS demographics when
     * absent. Template 120 maps to {@code UPPER_LIMB}, 121 to
     * {@code LOWER_LIMB}.
     *
     * @throws NotFoundException when the patient id is not numeric, the
     *                           document is not a 120/121 order of this
     *                           patient, or its URL answers HTTP 404
     */
    @Transactional
    public ProstheticsOrderResponse provisionFromMis(OrderProvisionRequest request) {
        if (request == null || request.getDocumentId() == null) {
            throw new NotFoundException("ID документа є обов'язковим");
        }
        Long numericPatientId = parsePatientId(request.getPatientId());
        List<DocumentMisDTO> documents = misService.getPatientDocuments(numericPatientId);
        DocumentMisDTO match = documents == null ? null : documents.stream()
                .filter(d -> request.getDocumentId().equals(d.getDocumentId()))
                .filter(d -> d.getDocumentTemplateId() != null
                        && ProstheticsEligibilityService.ELIGIBLE_DOCUMENT_TEMPLATE_IDS
                                .contains(d.getDocumentTemplateId()))
                .findFirst()
                .orElse(null);
        if (match == null) {
            throw new NotFoundException("Замовлення на протез не знайдено в MIS для пацієнта "
                    + request.getPatientId());
        }
        if (match.getDocumentUrl() == null || match.getDocumentUrl().isBlank()
                || !documentUrlAvailability.isAvailable(match.getDocumentUrl())) {
            throw new NotFoundException("Документ замовлення недоступний в MIS для пацієнта "
                    + request.getPatientId());
        }
        String orderNumber = "MIS-" + request.getPatientId() + "-" + request.getDocumentId();
        return orderRepository.findByOrderNumber(orderNumber)
                .map(orderMapper::toResponse)
                .orElseGet(() -> orderMapper.toResponse(orderRepository.save(ProstheticsOrder.builder()
                        .orderNumber(orderNumber)
                        .patient(ensureLocalPatient(request.getPatientId(), numericPatientId, match))
                        .productType(match.getDocumentTemplateId() == 120L
                                ? ProductType.UPPER_LIMB : ProductType.LOWER_LIMB)
                        .productCode(match.getProductCode())
                        .prescriptionDate(match.getOrderDate() == null
                                ? null : match.getOrderDate().toLocalDate())
                        .status(OrderStatus.NEW)
                        .build())));
    }

    private ProstheticsPatient ensureLocalPatient(
            String patientId, Long numericPatientId, DocumentMisDTO match) {
        return patientRepository.findById(patientId).orElseGet(() -> {
            PatientDTO mis = misService.getPatient(numericPatientId).orElse(null);
            String pib = mis != null && mis.getFullName() != null && !mis.getFullName().isBlank()
                    ? mis.getFullName()
                    : match.getPatientFullName();
            if (pib == null || pib.isBlank()) {
                pib = "Пацієнт " + patientId;
            }
            return patientRepository.save(ProstheticsPatient.builder()
                    .id(patientId)
                    .pib(pib)
                    .birthDate(mis != null && mis.getBirthDate() != null
                            ? mis.getBirthDate().toLocalDate() : null)
                    .gender(mis != null ? misSexToLabel(mis.getSexCode()) : null)
                    .phone(mis != null ? mis.getPhone() : null)
                    .email(mis != null ? mis.getEmail() : null)
                    .residence(mis != null ? mis.getAddress() : match.getPatientAddress())
                    .build());
        });
    }

    private static String misSexToLabel(String sexCode) {
        if (sexCode == null || sexCode.isBlank()) {
            return null;
        }
        return switch (sexCode.toUpperCase(java.util.Locale.ROOT)) {
            case "MAL" -> "Чоловіча";
            case "FEM" -> "Жіноча";
            default -> sexCode;
        };
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
