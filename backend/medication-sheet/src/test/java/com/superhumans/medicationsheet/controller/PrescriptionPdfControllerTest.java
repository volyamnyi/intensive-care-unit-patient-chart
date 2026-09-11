package com.superhumans.medicationsheet.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.mapper.MedicineCatalogMapperImpl;
import com.superhumans.medicationsheet.mapper.PrescriptionDayPartMapperImpl;
import com.superhumans.medicationsheet.mapper.PrescriptionItemMapperImpl;
import com.superhumans.medicationsheet.mapper.PrescriptionListMapperImpl;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfService;
import com.superhumans.medicationsheet.service.PrescriptionExecutionService;
import com.superhumans.medicationsheet.service.PrescriptionItemService;
import com.superhumans.medicationsheet.service.PrescriptionListService;
import com.superhumans.medicationsheet.service.VitalSignService;
import com.superhumans.mis.MisService;
import com.superhumans.service.AuditService;
import com.superhumans.service.PermissionService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrescriptionController.class)
@EnableTestExceptionHandler
@AutoConfigureMockMvc(addFilters = false)
@Import({
    com.superhumans.config.SecurityConfig.class,
    PrescriptionListMapperImpl.class,
    PrescriptionItemMapperImpl.class,
    PrescriptionDayPartMapperImpl.class,
    MedicineCatalogMapperImpl.class
})
class PrescriptionPdfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrescriptionListService listService;

    @MockitoBean
    private PrescriptionItemService itemService;

    @MockitoBean
    private PrescriptionExecutionService executionService;

    @MockitoBean
    private VitalSignService vitalSignService;

    @MockitoBean
    private PrescriptionPdfService prescriptionPdfService;

    @MockitoBean
    private MisService misService;

    @MockitoBean
    private com.superhumans.auth.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.superhumans.repository.core.AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean(name = "permissionService")
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        when(permissionService.has("PATIENT_VIEW")).thenReturn(true);
    }

    @Test
    void pdfInfoReturnsPagesAndFileName() throws Exception {
        UUID id = UUID.randomUUID();
        when(prescriptionPdfService.info(id))
                .thenReturn(new PrescriptionPdfService.PdfBatchInfo(2, "prescription-" + id + ".zip"));

        mockMvc.perform(get("/api/prescriptions/{id}/pdf/info", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pages").value(2))
                .andExpect(jsonPath("$.fileName").value("prescription-" + id + ".zip"));
    }

    @Test
    void pdfFileReturnsZipWithDeterministicEntries() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] page = "%PDF-1.4 fake".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (String name : new String[]{
                    "prescription-" + id + "-p01.pdf", "prescription-" + id + "-p02.pdf"}) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(page);
                zip.closeEntry();
            }
        }
        when(prescriptionPdfService.info(id))
                .thenReturn(new PrescriptionPdfService.PdfBatchInfo(2, "prescription-" + id + ".zip"));
        when(prescriptionPdfService.generateZip(eq(id), any())).thenReturn(out.toByteArray());

        MvcResult result = mockMvc.perform(get("/api/prescriptions/{id}/pdf/file", id)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Pages", "2"))
                .andExpect(header().string(
                        "Content-Disposition", "attachment; filename=\"prescription-" + id + ".zip\""))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body[0]).isEqualTo((byte) 'P');
        assertThat(body[1]).isEqualTo((byte) 'K');
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(body))) {
            ZipEntry first = zip.getNextEntry();
            assertThat(first.getName()).isEqualTo("prescription-" + id + "-p01.pdf");
            byte[] firstBytes = zip.readAllBytes();
            assertThat(new String(firstBytes, 0, 5, java.nio.charset.StandardCharsets.UTF_8))
                    .isEqualTo("%PDF-");
            ZipEntry second = zip.getNextEntry();
            assertThat(second.getName()).isEqualTo("prescription-" + id + "-p02.pdf");
            assertThat(zip.getNextEntry()).isNull();
        }
    }

    @Test
    void pdfPageReturnsSinglePdf() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] page = "%PDF-1.4 fake".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(prescriptionPdfService.generatePage(eq(id), eq(1), any())).thenReturn(page);

        MvcResult result = mockMvc.perform(
                        get("/api/prescriptions/{id}/pdf/pages/{pageIndex}", id, 1)
                                .with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"prescription-" + id + "-p02.pdf\""))
                .andReturn();

        assertThat(result.getResponse().getContentType()).contains("application/pdf");
        assertThat(new String(
                        result.getResponse().getContentAsByteArray(), 0, 5,
                        java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo("%PDF-");
    }

    @Test
    void pdfInfoForMissingListIs404() throws Exception {
        UUID id = UUID.randomUUID();
        when(prescriptionPdfService.info(id))
                .thenThrow(new NotFoundException("Prescription list not found: " + id));

        mockMvc.perform(get("/api/prescriptions/{id}/pdf/info", id)).andExpect(status().isNotFound());
    }

    @Test
    void pdfPageOutOfRangeIs404() throws Exception {
        UUID id = UUID.randomUUID();
        when(prescriptionPdfService.generatePage(eq(id), eq(9), any()))
                .thenThrow(new NotFoundException("Сторінку PDF не знайдено: 9"));

        mockMvc.perform(get("/api/prescriptions/{id}/pdf/pages/{pageIndex}", id, 9)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNotFound());
    }
}
