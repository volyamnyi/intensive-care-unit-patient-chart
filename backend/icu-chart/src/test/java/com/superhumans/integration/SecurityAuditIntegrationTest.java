package com.superhumans.integration;

import com.superhumans.entity.core.AuditEventEntity;
import com.superhumans.repository.core.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void login_recordsCanonicalSecurityEventInCoreStore() {
        String token = getAdminToken();
        assertThat(token).isNotNull();

        var events = auditEventRepository.findAll().stream()
                .filter(event -> "platform.auth.session.login".equals(event.getAction()))
                .filter(event -> adminUserId.toString().equals(event.getTargetId()))
                .toList();

        assertThat(events).isNotEmpty();
        AuditEventEntity event = events.get(events.size() - 1);
        assertThat(event.getEventClass()).isEqualTo("SECURITY");
        assertThat(event.getModule()).isEqualTo("platform");
        assertThat(event.getOutcome()).isEqualTo("SUCCESS");
        assertThat(event.getActorLogin()).isEqualTo("admin");
        assertThat(event.getTargetId()).isEqualTo(adminUserId.toString());
        assertThat(event.getRetentionClass()).isEqualTo("SECURITY");
        assertThat(event.getEventPayload()).contains("platform.auth.session.login");
    }

    @Test
    void failedLogin_recordsDeniedSecurityEventWithoutPassword() {
        java.time.Instant started = java.time.Instant.now().minusSeconds(5);
        var response = restTemplate.postForEntity(
                "/api/auth/login",
                new com.superhumans.dto.LoginRequest("admin", "wrong-password"),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        var events = auditEventRepository.findAll().stream()
                .filter(event -> "platform.auth.session.login.failed".equals(event.getAction()))
                .filter(event -> event.getOccurredAt() != null && !event.getOccurredAt().isBefore(started))
                .toList();

        assertThat(events).isNotEmpty();
        AuditEventEntity event = events.get(events.size() - 1);
        assertThat(event.getOutcome()).isEqualTo("FAILURE");
        assertThat(event.getEventPayload()).doesNotContain("wrong-password");
    }
}
