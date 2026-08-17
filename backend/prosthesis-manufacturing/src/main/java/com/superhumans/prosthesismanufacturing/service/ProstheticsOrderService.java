package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsPdfResponse;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsOrderMapper;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.service.AuditService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProstheticsOrderService {

    ProstheticsOrderRepository orderRepository;
    ProstheticsOrderMapper orderMapper;
    ProstheticsPdfService pdfService;
    MisOrderTemplateDataService misTemplateDataService;
    AuditService auditService;

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

    @Transactional
    public ProstheticsPdfResponse generateRecipe(UUID orderId, Long userId) {
        ProstheticsOrder order = load(orderId);
        MisOrderTemplateData misData = misTemplateDataService.load(patientIdOf(order));
        byte[] pdf = pdfService.generateOrderRecipe(order, misData);
        order.setRecipePdfData(pdf);
        order.setRecipePdfGeneratedAt(LocalDateTime.now());
        orderRepository.save(order);
        auditService.logAction("ProstheticsOrder", order.getId(), "RECIPE_GENERATE", userId);
        return ProstheticsPdfResponse.builder()
                .id(order.getId())
                .fileName("recipe_" + order.getOrderNumber() + ".pdf")
                .sizeBytes((long) pdf.length)
                .generatedBy(userId)
                .generatedAt(order.getRecipePdfGeneratedAt())
                .build();
    }

    @Transactional
    public PdfDocument getRecipePdf(UUID orderId, Long userId) {
        ProstheticsOrder order = load(orderId);
        if (order.getRecipePdfData() == null) {
            MisOrderTemplateData misData = misTemplateDataService.load(patientIdOf(order));
            byte[] pdf = pdfService.generateOrderRecipe(order, misData);
            order.setRecipePdfData(pdf);
            order.setRecipePdfGeneratedAt(LocalDateTime.now());
            orderRepository.save(order);
            auditService.logAction("ProstheticsOrder", order.getId(), "RECIPE_GENERATE", userId);
        }
        String fileName = "recipe_" + order.getOrderNumber() + ".pdf";
        return new PdfDocument(fileName, MediaType.APPLICATION_PDF_VALUE, order.getRecipePdfData());
    }

    @Transactional(readOnly = true)
    public ProstheticsPdfResponse getPdfInfo(UUID orderId) {
        ProstheticsOrder order = load(orderId);
        return ProstheticsPdfResponse.builder()
                .id(order.getId())
                .fileName("recipe_" + order.getOrderNumber() + ".pdf")
                .sizeBytes(order.getRecipePdfData() == null ? 0L : (long) order.getRecipePdfData().length)
                .generatedAt(order.getRecipePdfGeneratedAt())
                .build();
    }

    private ProstheticsOrder load(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    private String patientIdOf(ProstheticsOrder order) {
        return order.getPatient() == null ? null : order.getPatient().getId();
    }

    public record PdfDocument(String fileName, String mimeType, byte[] data) {
    }
}
