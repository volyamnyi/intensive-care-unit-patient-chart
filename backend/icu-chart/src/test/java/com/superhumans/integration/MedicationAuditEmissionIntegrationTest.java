package com.superhumans.integration;

import com.superhumans.entity.core.AuditEventEntity;
import com.superhumans.medicationsheet.dto.PrescriptionDoseRequest;
import com.superhumans.medicationsheet.dto.PrescriptionExecuteRequest;
import com.superhumans.medicationsheet.dto.PrescriptionItemAddRequest;
import com.superhumans.medicationsheet.dto.PrescriptionItemResponse;
import com.superhumans.medicationsheet.dto.PrescriptionListCreateRequest;
import com.superhumans.medicationsheet.dto.PrescriptionListResponse;
import com.superhumans.repository.core.AuditEventRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class MedicationAuditEmissionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Test
    void prescriptionFlow_emitsCanonicalEventsWithWitnessAndNoSecrets() {
        PrescriptionListCreateRequest listReq = new PrescriptionListCreateRequest();
        listReq.setPatientId("1003");
        var listRes = restTemplate.exchange("/api/prescriptions", HttpMethod.POST,
                authEntity(listReq, getDoctorToken()), PrescriptionListResponse.class);
        assertThat(listRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID listId = listRes.getBody().getId();

        PrescriptionItemAddRequest itemReq = new PrescriptionItemAddRequest();
        itemReq.setMedicineName("Auditflow Paracetamol");
        itemReq.setMedicineAtcCode("N02BE01");
        itemReq.setMedicineMethod("oral");
        itemReq.setRegime("test");
        var itemRes = restTemplate.exchange("/api/prescriptions/{listId}/items", HttpMethod.POST,
                authEntity(itemReq, getDoctorToken()), PrescriptionItemResponse.class, listId);
        assertThat(itemRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID itemId = itemRes.getBody().getId();
        var itemsRes = restTemplate.exchange("/api/prescriptions/{listId}/items", HttpMethod.GET,
                authGet(getDoctorToken()),
                new ParameterizedTypeReference<List<PrescriptionItemResponse>>() {}, listId);
        assertThat(itemsRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID dayPartId = itemsRes.getBody().stream()
                .filter(item -> itemId.equals(item.getId()))
                .flatMap(item -> item.getDayParts().stream())
                .filter(part -> !Boolean.TRUE.equals(part.getIsPlanned()))
                .map(part -> part.getId())
                .findFirst()
                .orElseThrow();

        PrescriptionDoseRequest planReq = new PrescriptionDoseRequest();
        planReq.setDose("500mg");
        var planRes = restTemplate.exchange("/api/prescriptions/day-parts/{dayPartId}/plan",
                HttpMethod.PUT, authEntity(planReq, getDoctorToken()),
                com.superhumans.medicationsheet.dto.PrescriptionDayPartResponse.class, dayPartId);
        assertThat(planRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        PrescriptionExecuteRequest execReq = new PrescriptionExecuteRequest();
        execReq.setActualDose("500mg");
        execReq.setSecondPersonLogin("nurse2");
        execReq.setSecondPersonPassword("nurse123");
        var execRes = restTemplate.exchange("/api/prescriptions/day-parts/{dayPartId}/execute",
                HttpMethod.POST, authEntity(execReq, getNurseToken()), String.class, dayPartId);
        assertThat(execRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        applicationContext.getBean(com.superhumans.service.AuditEventRelay.class).poll();

        List<AuditEventEntity> events = auditEventRepository.findAll();
        AuditEventEntity created = singleEvent(events, "medication.list.create", listId.toString());
        assertThat(created.getOutcome()).isEqualTo("SUCCESS");
        assertThat(created.getActorId()).isEqualTo(doctorUserId.toString());
        assertThat(created.getRequestId()).isNotNull();

        AuditEventEntity added = singleEvent(events, "medication.item.add", itemId.toString());
        assertThat(added.getOutcome()).isEqualTo("SUCCESS");

        AuditEventEntity planned = singleEvent(events, "medication.dose.plan", dayPartId.toString());
        assertThat(planned.getOutcome()).isEqualTo("SUCCESS");

        AuditEventEntity executed = singleEvent(events, "medication.dose.execute", dayPartId.toString());
        assertThat(executed.getOutcome()).isEqualTo("SUCCESS");
        assertThat(executed.getActorId()).isEqualTo(nurseUserId.toString());
        assertThat(executed.getEventPayload()).contains("nurse2");
        assertThat(executed.getEventPayload()).doesNotContain("nurse123");
    }

    private static AuditEventEntity singleEvent(
            List<AuditEventEntity> events, String action, String targetId) {
        var matches = events.stream()
                .filter(event -> action.equals(event.getAction()) && targetId.equals(event.getTargetId()))
                .toList();
        assertThat(matches).as("single canonical event %s for %s", action, targetId).hasSize(1);
        return matches.get(0);
    }
}
