package com.superhumans.integration;

import com.superhumans.dto.EpisodeCreateRequest;
import com.superhumans.dto.EpisodeResponse;
import com.superhumans.entity.core.AuditEventEntity;
import com.superhumans.repository.core.AuditEventRepository;
import com.superhumans.repository.core.AuditEventTargetRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class IcuAuditEmissionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditEventTargetRepository auditEventTargetRepository;

    @Test
    void createEpisode_emitsCanonicalRootEventWithDayRelation() throws Exception {
        EpisodeCreateRequest epReq = new EpisodeCreateRequest(
                1031L, null, null, LocalDateTime.now(), null, null, null, null, null);
        var epRes = restTemplate.exchange("/api/episodes", HttpMethod.POST,
                authEntity(epReq, getDoctorToken()), EpisodeResponse.class);

        assertThat(epRes.getStatusCode().is2xxSuccessful()).isTrue();
        UUID newEpisodeId = epRes.getBody().getId();

        auditEventRelayPoll();
        var events = auditEventRepository.findAll().stream()
                .filter(event -> "icu.episode.create".equals(event.getAction()))
                .filter(event -> newEpisodeId.toString().equals(event.getTargetId()))
                .toList();

        assertThat(events).hasSize(1);
        AuditEventEntity event = events.get(0);
        assertThat(event.getEventClass()).isEqualTo("BUSINESS");
        assertThat(event.getOutcome()).isEqualTo("SUCCESS");
        assertThat(event.getActorId()).isEqualTo(doctorUserId.toString());
        assertThat(event.getRequestId()).isNotNull();
        assertThat(event.getCorrelationId()).isNotNull();
        assertThat(event.getEventPayload()).contains("icu.episode.create");
        assertThat(event.getBusinessKey()).isEqualTo("1031");

        var targets = auditEventTargetRepository
                .findByEntityTypeAndEntityIdOrderByOccurredAtDesc("Episode", newEpisodeId.toString());
        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).getRelationType()).isEqualTo("PRIMARY");

        String dayId = lookupDayId(event);
        var dayTargets = auditEventTargetRepository
                .findByEntityTypeAndEntityIdOrderByOccurredAtDesc("ClinicalDay", dayId);
        assertThat(dayTargets).hasSize(1);
        assertThat(dayTargets.get(0).getRelationType()).isEqualTo("RELATED");
        assertThat(dayTargets.get(0).getAuditId()).isEqualTo(event.getAuditId());
    }

    private String lookupDayId(AuditEventEntity event) throws Exception {
        var payload = new tools.jackson.databind.ObjectMapper().readTree(event.getEventPayload());
        for (var related : payload.get("relatedEntities")) {
            if ("ClinicalDay".equals(related.get("entityType").asText())) {
                return related.get("entityId").asText();
            }
        }
        throw new AssertionError("No ClinicalDay relation in audit payload");
    }

    private void auditEventRelayPoll() {
        // The relay sweep also runs on schedule; invoke it directly for determinism.
        // Bean lookup by type keeps this test independent of relay scheduling config.
        applicationContext.getBean(com.superhumans.service.AuditEventRelay.class).poll();
    }

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;
}
