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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Setup step 2 → execution chain: picking an MIS limb-order document
 * provisions (find-or-create) the local order, so selection never depends
 * on pre-existing local rows.
 */
@ExtendWith(MockitoExtension.class)
class ProstheticsOrderProvisionTest {

    @Mock
    private ProstheticsOrderRepository orderRepository;
    @Mock
    private ProstheticsOrderMapper orderMapper;
    @Mock
    private MisService misService;
    @Mock
    private DocumentUrlAvailability documentUrlAvailability;
    @Mock
    private ProstheticsPatientRepository patientRepository;

    @InjectMocks
    private ProstheticsOrderService orderService;

    private static DocumentMisDTO misDoc(long documentId, long templateId, String url) {
        return DocumentMisDTO.builder()
                .documentId(documentId)
                .documentTemplateId(templateId)
                .documentTemplateName(templateId == 121L
                        ? "Замовлення на протези нижніх кінцівок"
                        : "Замовлення на протези верхніх кінцівок")
                .documentUrl(url)
                .patientId(13373L)
                .patientFullName("Бондаренко Тетяна Тестівна")
                .productCode("06 24 09")
                .orderDate(LocalDateTime.of(2026, 9, 7, 0, 0, 0))
                .build();
    }

    private static PatientDTO misPatient() {
        return PatientDTO.builder()
                .id(13373L)
                .fullName("Бондаренко Тетяна Тестівна")
                .birthDate(LocalDateTime.of(1990, 5, 12, 0, 0, 0))
                .sexCode("FEM")
                .phone("380501112233")
                .email("t.bond@mail.com")
                .address("м. Одеса")
                .departmentId(19L)
                .build();
    }

    private static OrderProvisionRequest request() {
        return OrderProvisionRequest.builder().patientId("13373").documentId(681078L).build();
    }

    @Test
    void createsPatientAndLowerLimbOrder_fromMisDocument() {
        when(misService.getPatientDocuments(13373L))
                .thenReturn(List.of(misDoc(681078L, 121L, "https://mis/docs/1")));
        when(documentUrlAvailability.isAvailable("https://mis/docs/1")).thenReturn(true);
        when(orderRepository.findByOrderNumber("MIS-13373-681078")).thenReturn(Optional.empty());
        when(patientRepository.findById("13373")).thenReturn(Optional.empty());
        when(misService.getPatient(13373L)).thenReturn(Optional.of(misPatient()));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID id = UUID.randomUUID();
        when(orderMapper.toResponse(any())).thenReturn(
                ProstheticsOrderResponse.builder().id(id).orderNumber("MIS-13373-681078").build());

        ProstheticsOrderResponse res = orderService.provisionFromMis(request());

        assertThat(res.getOrderNumber()).isEqualTo("MIS-13373-681078");
        ArgumentCaptor<ProstheticsPatient> patientCap =
                ArgumentCaptor.forClass(ProstheticsPatient.class);
        verify(patientRepository).save(patientCap.capture());
        assertThat(patientCap.getValue().getId()).isEqualTo("13373");
        assertThat(patientCap.getValue().getPib()).isEqualTo("Бондаренко Тетяна Тестівна");
        ArgumentCaptor<ProstheticsOrder> orderCap =
                ArgumentCaptor.forClass(ProstheticsOrder.class);
        verify(orderRepository).save(orderCap.capture());
        assertThat(orderCap.getValue().getProductType()).isEqualTo(ProductType.LOWER_LIMB);
        assertThat(orderCap.getValue().getProductCode()).isEqualTo("06 24 09");
        assertThat(orderCap.getValue().getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(orderCap.getValue().getPrescriptionDate())
                .isEqualTo(java.time.LocalDate.of(2026, 9, 7));
    }

    @Test
    void mapsTemplate120_toUpperLimb() {
        when(misService.getPatientDocuments(13373L))
                .thenReturn(List.of(misDoc(681078L, 120L, "https://mis/docs/1")));
        when(documentUrlAvailability.isAvailable("https://mis/docs/1")).thenReturn(true);
        when(orderRepository.findByOrderNumber("MIS-13373-681078")).thenReturn(Optional.empty());
        when(patientRepository.findById("13373")).thenReturn(Optional.of(
                ProstheticsPatient.builder().id("13373").pib("Бондаренко Тетяна Тестівна").build()));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any())).thenReturn(ProstheticsOrderResponse.builder().build());

        orderService.provisionFromMis(request());

        ArgumentCaptor<ProstheticsOrder> orderCap =
                ArgumentCaptor.forClass(ProstheticsOrder.class);
        verify(orderRepository).save(orderCap.capture());
        assertThat(orderCap.getValue().getProductType()).isEqualTo(ProductType.UPPER_LIMB);
        verify(patientRepository, never()).save(any());
    }

    @Test
    void reusesExistingOrder_withoutSaving() {
        ProstheticsOrder existing = ProstheticsOrder.builder().orderNumber("MIS-13373-681078").build();
        when(misService.getPatientDocuments(13373L))
                .thenReturn(List.of(misDoc(681078L, 121L, "https://mis/docs/1")));
        when(documentUrlAvailability.isAvailable("https://mis/docs/1")).thenReturn(true);
        when(orderRepository.findByOrderNumber("MIS-13373-681078"))
                .thenReturn(Optional.of(existing));
        when(orderMapper.toResponse(existing)).thenReturn(
                ProstheticsOrderResponse.builder().orderNumber("MIS-13373-681078").build());

        ProstheticsOrderResponse res = orderService.provisionFromMis(request());

        assertThat(res.getOrderNumber()).isEqualTo("MIS-13373-681078");
        verify(orderRepository, never()).save(any());
        verify(patientRepository, never()).save(any());
    }

    @Test
    void unknownDocumentId_throwsNotFound() {
        when(misService.getPatientDocuments(13373L))
                .thenReturn(List.of(misDoc(999999L, 121L, "https://mis/docs/9")));

        assertThatThrownBy(() -> orderService.provisionFromMis(request()))
                .isInstanceOf(NotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void foreignTemplate_throwsNotFound() {
        when(misService.getPatientDocuments(13373L))
                .thenReturn(List.of(misDoc(681078L, 999L, "https://mis/docs/9")));

        assertThatThrownBy(() -> orderService.provisionFromMis(request()))
                .isInstanceOf(NotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deadUrl_throwsNotFound() {
        when(misService.getPatientDocuments(13373L))
                .thenReturn(List.of(misDoc(681078L, 121L, "https://mis/docs/9")));
        when(documentUrlAvailability.isAvailable("https://mis/docs/9")).thenReturn(false);

        assertThatThrownBy(() -> orderService.provisionFromMis(request()))
                .isInstanceOf(NotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void nonNumericPatientId_throwsNotFoundWithoutMisCall() {
        assertThatThrownBy(() -> orderService.provisionFromMis(
                        OrderProvisionRequest.builder().patientId("abc").documentId(1L).build()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> orderService.provisionFromMis(null))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> orderService.provisionFromMis(
                        OrderProvisionRequest.builder().patientId("13373").documentId(null).build()))
                .isInstanceOf(NotFoundException.class);

        verify(misService, never()).getPatientDocuments(any());
        verify(orderRepository, never()).save(any());
    }
}
