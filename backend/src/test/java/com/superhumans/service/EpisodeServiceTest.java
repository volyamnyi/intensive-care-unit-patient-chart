package com.superhumans.service;

import com.superhumans.dto.EpisodeCloseRequest;
import com.superhumans.dto.EpisodeCreateRequest;
import com.superhumans.dto.EpisodePatchRequest;
import com.superhumans.dto.EpisodeResponse;
import com.superhumans.entity.Episode;
import com.superhumans.entity.EpisodeStatus;
import com.superhumans.exception.EpisodeAlreadyActiveException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.EpisodeMapper;
import com.superhumans.mis.MisService;
import com.superhumans.repository.EpisodeRepository;
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
class EpisodeServiceTest {

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private MisService misService;

    @Mock
    private EpisodeMapper episodeMapper;

    @InjectMocks
    private EpisodeService episodeService;

    @Captor
    private ArgumentCaptor<Episode> episodeCaptor;

    private UUID episodeId;
    private Long patientId;
    private Long userId;
    private Episode testEpisode;

    @BeforeEach
    void setUp() {
        episodeId = UUID.randomUUID();
        patientId = 1001L;
        userId = 11L;
        testEpisode = Episode.builder()
                .patientId(patientId)
                .admissionDate(LocalDateTime.now())
                .status(EpisodeStatus.ACTIVE)
                .build();
        testEpisode.setId(episodeId);
        testEpisode.setVersion(0);
    }

    @Test
    void getEpisode_whenFound_returnsResponse() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(testEpisode));

        EpisodeResponse expected = EpisodeResponse.builder()
                .id(episodeId)
                .patientId(patientId)
                .status(EpisodeStatus.ACTIVE)
                .build();
        when(episodeMapper.toResponse(any(Episode.class), any())).thenReturn(expected);

        EpisodeResponse res = episodeService.getEpisode(episodeId);

        assertThat(res.getId()).isEqualTo(episodeId);
        assertThat(res.getPatientId()).isEqualTo(patientId);
        assertThat(res.getStatus()).isEqualTo(EpisodeStatus.ACTIVE);
    }

    @Test
    void getEpisode_whenNotFound_throws() {
        when(episodeRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> episodeService.getEpisode(episodeId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchEpisodes_withPatientIdAndStatus_filters() {
        when(episodeRepository.findByPatientIdAndStatus(patientId, EpisodeStatus.ACTIVE))
                .thenReturn(Optional.of(testEpisode));

        List<EpisodeResponse> results = episodeService.searchEpisodes(patientId, EpisodeStatus.ACTIVE);

        assertThat(results).hasSize(1);
    }

    @Test
    void searchEpisodes_withPatientIdOnly_filters() {
        when(episodeRepository.findByPatientId(patientId)).thenReturn(List.of(testEpisode));

        List<EpisodeResponse> results = episodeService.searchEpisodes(patientId, null);

        assertThat(results).hasSize(1);
    }

    @Test
    void searchEpisodes_withStatusOnly_filters() {
        when(episodeRepository.findByStatus(EpisodeStatus.ACTIVE)).thenReturn(List.of(testEpisode));

        List<EpisodeResponse> results = episodeService.searchEpisodes(null, EpisodeStatus.ACTIVE);

        assertThat(results).hasSize(1);
    }

    @Test
    void searchEpisodes_withoutFilters_returnsAll() {
        when(episodeRepository.findAll()).thenReturn(List.of(testEpisode));

        List<EpisodeResponse> results = episodeService.searchEpisodes(null, null);

        assertThat(results).hasSize(1);
    }

    @Test
    void createEpisode_createsAndReturns() {
        EpisodeCreateRequest req = new EpisodeCreateRequest(patientId, null, null, LocalDateTime.now());
        when(episodeRepository.findByPatientIdAndStatus(patientId, EpisodeStatus.ACTIVE))
                .thenReturn(Optional.empty());
        Episode entity = Episode.builder().patientId(patientId).build();
        when(episodeMapper.toEntity(req)).thenReturn(entity);
        Episode saved = Episode.builder().patientId(patientId).status(EpisodeStatus.ACTIVE).build();
        saved.setId(episodeId);
        saved.setVersion(0);
        when(episodeRepository.save(any(Episode.class))).thenReturn(saved);

        EpisodeResponse expected = EpisodeResponse.builder()
                .id(episodeId)
                .patientId(patientId)
                .status(EpisodeStatus.ACTIVE)
                .build();
        when(episodeMapper.toResponse(any(Episode.class))).thenReturn(expected);

        EpisodeResponse res = episodeService.createEpisode(req, userId);

        assertThat(res.getPatientId()).isEqualTo(patientId);
        assertThat(res.getStatus()).isEqualTo(EpisodeStatus.ACTIVE);
        verify(auditService).logCreate("Episode", episodeId, userId);
    }

    @Test
    void createEpisode_whenActiveExists_throws() {
        EpisodeCreateRequest req = new EpisodeCreateRequest(patientId, null, null, LocalDateTime.now());
        when(episodeRepository.findByPatientIdAndStatus(patientId, EpisodeStatus.ACTIVE))
                .thenReturn(Optional.of(testEpisode));

        assertThatThrownBy(() -> episodeService.createEpisode(req, userId))
                .isInstanceOf(EpisodeAlreadyActiveException.class);
        verify(episodeRepository, never()).save(any());
    }

    @Test
    void updateEpisode_withVersionMismatch_throws() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(testEpisode));
        EpisodePatchRequest req = new EpisodePatchRequest(null, null, null, 999);

        assertThatThrownBy(() -> episodeService.updateEpisode(episodeId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void updateEpisode_updatesFields() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(testEpisode));
        Episode saved = Episode.builder().status(EpisodeStatus.ACTIVE).build();
        saved.setId(episodeId);
        saved.setVersion(1);
        when(episodeRepository.save(any(Episode.class))).thenReturn(saved);

        EpisodePatchRequest req = new EpisodePatchRequest(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), 0);

        when(episodeMapper.toResponse(any(Episode.class))).thenReturn(
                EpisodeResponse.builder().id(episodeId).build());

        EpisodeResponse res = episodeService.updateEpisode(episodeId, req, userId);

        assertThat(res).isNotNull();
        verify(auditService).logUpdate("Episode", episodeId, userId, null, "Updated episode fields");
    }

    @Test
    void closeEpisode_withVersionMismatch_throws() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(testEpisode));
        EpisodeCloseRequest req = new EpisodeCloseRequest(LocalDateTime.now(), 999);

        assertThatThrownBy(() -> episodeService.closeEpisode(episodeId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void closeEpisode_setsStatusCompleted() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(testEpisode));
        Episode saved = Episode.builder().status(EpisodeStatus.COMPLETED).build();
        saved.setId(episodeId);
        saved.setVersion(1);
        when(episodeRepository.save(any(Episode.class))).thenReturn(saved);

        EpisodeCloseRequest req = new EpisodeCloseRequest(LocalDateTime.now(), 0);

        EpisodeResponse res = episodeService.closeEpisode(episodeId, req, userId);

        verify(episodeRepository).save(episodeCaptor.capture());
        assertThat(episodeCaptor.getValue().getStatus()).isEqualTo(EpisodeStatus.COMPLETED);
        assertThat(episodeCaptor.getValue().getDischargeDate()).isEqualTo(req.getDischargeDate());
        verify(auditService).logAction("Episode", episodeId, "CLOSE", userId);
    }
}
