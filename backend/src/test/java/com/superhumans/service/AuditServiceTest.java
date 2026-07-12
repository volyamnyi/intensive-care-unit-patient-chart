package com.superhumans.service;

import com.superhumans.entity.AuditLog;
import com.superhumans.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private AuditService auditService;

    @Test
    void log_shouldSaveAuditEntry() {
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        auditService.log("doctor1", "CREATE_CARD", "IcuCard", 1L, "details", "127.0.0.1");
        verify(auditLogRepository).save(argThat(log ->
                "doctor1".equals(log.getUserId()) &&
                "CREATE_CARD".equals(log.getAction()) &&
                "IcuCard".equals(log.getEntityType()) &&
                1L == log.getEntityId() &&
                "details".equals(log.getDetails()) &&
                "127.0.0.1".equals(log.getIpAddress()) &&
                log.getCreatedAt() != null
        ));
    }

    @Test
    void log_shouldHandleLongDetails() {
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        String longDetails = "a".repeat(1000);
        auditService.log("doctor1", "UPDATE_CARD", "IcuCard", 1L, longDetails, "127.0.0.1");
        verify(auditLogRepository).save(argThat(log -> longDetails.equals(log.getDetails())));
    }

    @Test
    void log_shouldAllowNullDetailsAndIp() {
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        auditService.log("nurse1", "EXECUTE_PRESCRIPTION", "FluidIntake", 5L, null, null);
        verify(auditLogRepository).save(argThat(log ->
                "nurse1".equals(log.getUserId()) &&
                log.getDetails() == null &&
                log.getIpAddress() == null
        ));
    }

    @Test
    void log_shouldRecordBackdatedEntry_withHourDifference() {
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        auditService.log("nurse1", "BACKDATE_VITALS", "HourlyVital", 10L,
                "Backdated: entered at hour 14 for hour 8, diff 6h", "192.168.1.1");
        verify(auditLogRepository).save(argThat(log ->
                "BACKDATE_VITALS".equals(log.getAction()) &&
                log.getDetails().contains("Backdated") &&
                log.getDetails().contains("diff 6h")
        ));
    }
}
