package com.superhumans.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.superhumans.dto.AuditLogResponse;
import com.superhumans.entity.AuditLog;
import com.superhumans.exception.NotFoundException;
import com.superhumans.mapper.AuditLogMapper;
import com.superhumans.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditService auditService;

    @Captor
    private ArgumentCaptor<AuditLog> logCaptor;

    private UUID logId;
    private UUID entityId;
    private Long userId;

    @BeforeEach
    void setUp() {
        logId = UUID.randomUUID();
        entityId = UUID.randomUUID();
        userId = 11L;
    }

    @Test
    void getAuditLog_whenFound_returnsResponse() {
        AuditLog log = AuditLog.builder()
                .id(logId)
                .entity("Episode")
                .entityId(entityId)
                .action("CREATE")
                .userId(userId)
                .build();
        when(auditLogRepository.findById(logId)).thenReturn(Optional.of(log));

        AuditLogResponse expected = AuditLogResponse.builder()
                .id(logId)
                .entity("Episode")
                .action("CREATE")
                .build();
        when(auditLogMapper.toResponse(log)).thenReturn(expected);

        AuditLogResponse result = auditService.getAuditLog(logId);

        assertThat(result.getId()).isEqualTo(logId);
        assertThat(result.getEntity()).isEqualTo("Episode");
        assertThat(result.getAction()).isEqualTo("CREATE");
    }

    @Test
    void getAuditLog_whenNotFound_throws() {
        when(auditLogRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getAuditLog(logId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAuditLogs_noFilters_returnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findAllByOrderByTimestampDesc(pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLogResponse> result = auditService.getAuditLogs(null, null, null, null, null, null, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void getAuditLogs_filterByUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findByUserIdOrderByTimestampDesc(eq(userId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLogResponse> result = auditService.getAuditLogs(userId, null, null, null, null, null, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void getAuditLogs_filterByEntityAndEntityId() {
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findByEntityAndEntityIdOrderByTimestampDesc(
                eq("Episode"), eq(entityId), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLogResponse> result = auditService.getAuditLogs(null, "Episode", entityId, null, null, null, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void getAuditLogs_filterByEntity() {
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findByEntityOrderByTimestampDesc(eq("Episode"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLogResponse> result = auditService.getAuditLogs(null, "Episode", null, null, null, null, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void getAuditLogs_filterByAction() {
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findByActionOrderByTimestampDesc(eq("CREATE"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLogResponse> result = auditService.getAuditLogs(null, null, null, "CREATE", null, null, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void getAuditLogs_filterByDateRange() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        when(auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(eq(from), eq(to), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLogResponse> result = auditService.getAuditLogs(null, null, null, null, from, to, pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void list_returnsPageWithContentAndPageable() {
        Pageable pageable = PageRequest.of(0, 10);
        List<AuditLog> content = List.of(AuditLog.builder()
                .id(logId)
                .entity("Episode")
                .action("CREATE")
                .userId(userId)
                .build());
        when(auditLogRepository.findAllByOrderByTimestampDesc(pageable))
                .thenReturn(new PageImpl<>(content, pageable, content.size()));

        AuditLogResponse mapped = AuditLogResponse.builder()
                .id(logId)
                .entity("Episode")
                .action("CREATE")
                .build();
        when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(mapped);

        Page<AuditLogResponse> result = auditService.getAuditLogs(null, null, null, null, null, null, pageable);

        assertThat(result).isInstanceOf(Page.class);
        assertThat(result.getContent()).isInstanceOf(List.class);
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(0);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(logId);
    }

    @Test
    void logCreate_savesEntry() {
        auditService.logCreate("Episode", entityId, userId);

        verify(auditLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getEntity()).isEqualTo("Episode");
        assertThat(logCaptor.getValue().getEntityId()).isEqualTo(entityId);
        assertThat(logCaptor.getValue().getAction()).isEqualTo("CREATE");
        assertThat(logCaptor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    void logUpdate_setsOldAndNewValues() {
        auditService.logUpdate("Episode", entityId, userId, "old state", "new state");

        verify(auditLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo("UPDATE");
        assertThat(logCaptor.getValue().getOldValue()).isEqualTo("old state");
        assertThat(logCaptor.getValue().getNewValue()).isEqualTo("new state");
    }

    @Test
    void logDelete_savesEntry() {
        auditService.logDelete("Episode", entityId, userId);

        verify(auditLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo("DELETE");
    }

    @Test
    void logAction_savesEntry() {
        auditService.logAction("Episode", entityId, "CANCEL", userId);

        verify(auditLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo("CANCEL");
    }
}
