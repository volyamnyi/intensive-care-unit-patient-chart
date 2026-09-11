package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsOrderMapper;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Setup step 2 (order selection): {@code spiDocumentProsthesCheck} documents
 * narrow to limb-prosthesis templates with live URLs.
 */
@ExtendWith(MockitoExtension.class)
class ProstheticsOrderDocumentsTest {

    @Mock
    private ProstheticsOrderRepository orderRepository;
    @Mock
    private ProstheticsOrderMapper orderMapper;
    @Mock
    private MisService misService;
    @Mock
    private DocumentUrlAvailability documentUrlAvailability;

    @InjectMocks
    private ProstheticsOrderService orderService;

    private static DocumentMisDTO doc(long templateId, String url) {
        return DocumentMisDTO.builder()
                .documentId(templateId * 1000)
                .documentTemplateId(templateId)
                .documentTemplateName(templateId == 121L
                        ? "Замовлення на протези нижніх кінцівок"
                        : "Замовлення на протези верхніх кінцівок")
                .documentUrl(url)
                .build();
    }

    @Test
    void returnsLowerAndUpperLimbOrders_preservingMisOrder() {
        when(documentUrlAvailability.isAvailable(any())).thenReturn(true);
        when(misService.getPatientDocuments(13373L)).thenReturn(List.of(
                doc(121L, "https://mis/docs/1"),
                doc(120L, "https://mis/docs/2")));

        List<DocumentMisDTO> res = orderService.getAllLowerLimbsOrdersForByPatientId("13373");

        assertThat(res).extracting(DocumentMisDTO::getDocumentTemplateId)
                .containsExactly(121L, 120L);
        verify(misService).getPatientDocuments(13373L);
    }

    @Test
    void excludesForeignTemplates_nullTemplate_blankUrl() {
        when(documentUrlAvailability.isAvailable(any())).thenReturn(true);
        DocumentMisDTO nullTemplate = DocumentMisDTO.builder()
                .documentId(7L).documentUrl("https://mis/docs/7").build();
        DocumentMisDTO blankUrl = DocumentMisDTO.builder()
                .documentId(8L).documentTemplateId(121L).documentUrl("  ").build();
        when(misService.getPatientDocuments(13373L)).thenReturn(List.of(
                doc(999L, "https://mis/docs/9"),
                nullTemplate,
                blankUrl,
                doc(121L, "https://mis/docs/1")));

        List<DocumentMisDTO> res = orderService.getAllLowerLimbsOrdersForByPatientId("13373");

        assertThat(res).extracting(DocumentMisDTO::getDocumentId).containsExactly(121000L);
    }

    @Test
    void excludesDocumentsWhoseUrlAnswers404() {
        when(misService.getPatientDocuments(13373L)).thenReturn(List.of(
                doc(121L, "https://mis/docs/1"),
                doc(121L, "https://mis/docs/2")));
        when(documentUrlAvailability.isAvailable("https://mis/docs/1")).thenReturn(true);
        when(documentUrlAvailability.isAvailable("https://mis/docs/2")).thenReturn(false);

        List<DocumentMisDTO> res = orderService.getAllLowerLimbsOrdersForByPatientId("13373");

        assertThat(res).extracting(DocumentMisDTO::getDocumentUrl)
                .containsExactly("https://mis/docs/1");
    }

    @Test
    void emptyOrNullMisList_returnsEmpty() {
        when(misService.getPatientDocuments(13373L)).thenReturn(List.of());
        assertThat(orderService.getAllLowerLimbsOrdersForByPatientId("13373")).isEmpty();

        when(misService.getPatientDocuments(13374L)).thenReturn(null);
        assertThat(orderService.getAllLowerLimbsOrdersForByPatientId("13374")).isEmpty();
    }

    @Test
    void preservesOrderNumber_fromMisSource() {
        when(documentUrlAvailability.isAvailable(any())).thenReturn(true);
        DocumentMisDTO withNumber = DocumentMisDTO.builder()
                .documentId(681078L)
                .documentTemplateId(121L)
                .documentTemplateName("Замовлення на протези нижніх кінцівок")
                .documentUrl("https://mis/docs/1")
                .orderNumber("BZ-2026-0042")
                .build();
        when(misService.getPatientDocuments(13373L)).thenReturn(List.of(withNumber));

        List<DocumentMisDTO> res = orderService.getAllLowerLimbsOrdersForByPatientId("13373");

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getOrderNumber()).isEqualTo("BZ-2026-0042");
    }

    @Test
    void nonNumericPatientId_throwsNotFoundWithoutMisCall() {
        assertThatThrownBy(() -> orderService.getAllLowerLimbsOrdersForByPatientId("abc"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> orderService.getAllLowerLimbsOrdersForByPatientId(null))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> orderService.getAllLowerLimbsOrdersForByPatientId("1337 3"))
                .isInstanceOf(NotFoundException.class);

        verify(misService, never()).getPatientDocuments(any());
    }
}
