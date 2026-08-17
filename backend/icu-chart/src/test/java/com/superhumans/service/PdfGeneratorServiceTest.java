package com.superhumans.service;

import com.superhumans.dto.PdfResponse;
import com.superhumans.icu.entity.*;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.EpisodeRepository;
import com.superhumans.icu.repository.FluidBalanceRepository;
import com.superhumans.icu.repository.GeneratedPdfRepository;
import com.superhumans.icu.repository.HourlyRecordRepository;
import com.superhumans.icu.repository.MedicalNoteRepository;
import com.superhumans.icu.repository.MedicalOrderRepository;
import com.superhumans.icu.repository.OrderExecutionRepository;
import com.superhumans.icu.repository.ScaleResultRepository;
import com.superhumans.icu.repository.SignatureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfGeneratorServiceTest {

    @Mock
    private GeneratedPdfRepository generatedPdfRepository;

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private HourlyRecordRepository hourlyRecordRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private MedicalOrderRepository medicalOrderRepository;

    @Mock
    private OrderExecutionRepository orderExecutionRepository;

    @Mock
    private MedicalNoteRepository medicalNoteRepository;

    @Mock
    private ScaleResultRepository scaleResultRepository;

    @Mock
    private FluidBalanceRepository fluidBalanceRepository;

    @Mock
    private SignatureRepository signatureRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.superhumans.mis.MisService misService;

    @InjectMocks
    private PdfGeneratorService pdfGeneratorService;

    @Captor
    private ArgumentCaptor<GeneratedPdf> pdfCaptor;

    private UUID clinicalDayId;
    private ClinicalDay clinicalDay;
    private Episode episode;
    private Long userId;

    @BeforeEach
    void setUp() {
        clinicalDayId = UUID.randomUUID();
        userId = 11L;
        episode = new Episode();
        episode.setId(UUID.randomUUID());
        episode.setAdmissionDate(LocalDateTime.now());
        episode.setStatus(EpisodeStatus.ACTIVE);

        clinicalDay = new ClinicalDay();
        clinicalDay.setId(clinicalDayId);
        clinicalDay.setEpisode(episode);
        clinicalDay.setDayNumber(1);
        clinicalDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        clinicalDay.setStartDateTime(LocalDateTime.now());
        clinicalDay.setEndDateTime(LocalDateTime.now().plusHours(24));
    }

    @Test
    void getLatestPdf_whenFound_returnsResponse() {
        GeneratedPdf pdf = new GeneratedPdf();
        pdf.setId(UUID.randomUUID());
        pdf.setClinicalDay(clinicalDay);
        pdf.setFileName("test.pdf");
        pdf.setFileVersion(1);
        pdf.setGeneratedAt(LocalDateTime.now());
        pdf.setGeneratedBy(userId);

        when(generatedPdfRepository.findFirstByClinicalDayIdOrderByFileVersionDesc(clinicalDayId))
                .thenReturn(Optional.of(pdf));

        PdfResponse res = pdfGeneratorService.getLatestPdf(clinicalDayId);

        assertThat(res.getFileName()).isEqualTo("test.pdf");
        assertThat(res.getFileVersion()).isEqualTo(1);
    }

    @Test
    void getLatestPdf_whenNotFound_throws() {
        when(generatedPdfRepository.findFirstByClinicalDayIdOrderByFileVersionDesc(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdfGeneratorService.getLatestPdf(clinicalDayId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void generatePdf_createsAndSavesPdfRecord() {
        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(generatedPdfRepository.findFirstByClinicalDayIdOrderByFileVersionDesc(clinicalDayId))
                .thenReturn(Optional.empty());
        when(misService.sendPdf(any(), any(), any(), anyInt())).thenReturn(true);

        GeneratedPdf saved = new GeneratedPdf();
        saved.setId(UUID.randomUUID());
        saved.setClinicalDay(clinicalDay);
        saved.setFileName("test.pdf");
        saved.setFileVersion(1);
        saved.setGeneratedAt(LocalDateTime.now());
        saved.setGeneratedBy(userId);
        when(generatedPdfRepository.save(any(GeneratedPdf.class))).thenReturn(saved);

        PdfResponse res = pdfGeneratorService.generatePdf(clinicalDayId, userId);

        verify(generatedPdfRepository, times(2)).save(pdfCaptor.capture());
        assertThat(pdfCaptor.getValue().getFileVersion()).isEqualTo(1);
        assertThat(pdfCaptor.getValue().getGeneratedBy()).isEqualTo(userId);
        verify(auditService).logAction("GeneratedPdf", clinicalDayId, "GENERATE", userId);
    }

    @Test
    void generatePdf_whenDayIsOpen_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.OPEN);
        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));

        assertThatThrownBy(() -> pdfGeneratorService.generatePdf(clinicalDayId, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void generatePdf_whenDayIsNurseSigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.NURSE_SIGNED);
        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));

        assertThatThrownBy(() -> pdfGeneratorService.generatePdf(clinicalDayId, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void generatePdf_incrementsVersion() {
        GeneratedPdf existing = new GeneratedPdf();
        existing.setId(UUID.randomUUID());
        existing.setClinicalDay(clinicalDay);
        existing.setFileVersion(1);

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(generatedPdfRepository.findFirstByClinicalDayIdOrderByFileVersionDesc(clinicalDayId))
                .thenReturn(Optional.of(existing));
        when(misService.sendPdf(any(), any(), any(), anyInt())).thenReturn(true);

        GeneratedPdf saved = new GeneratedPdf();
        saved.setId(UUID.randomUUID());
        saved.setClinicalDay(clinicalDay);
        saved.setFileVersion(2);
        when(generatedPdfRepository.save(any(GeneratedPdf.class))).thenReturn(saved);

        PdfResponse res = pdfGeneratorService.generatePdf(clinicalDayId, userId);

        verify(generatedPdfRepository, times(2)).save(pdfCaptor.capture());
        assertThat(pdfCaptor.getValue().getFileVersion()).isEqualTo(2);
    }
}
