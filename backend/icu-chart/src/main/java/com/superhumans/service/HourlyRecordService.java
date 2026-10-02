package com.superhumans.service;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditChanges;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.dto.HourlyRecordCreateRequest;
import com.superhumans.dto.HourlyRecordPatchRequest;
import com.superhumans.dto.HourlyRecordResponse;
import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.ClinicalDayStatus;
import com.superhumans.icu.entity.HourlyRecord;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.DuplicateHourlyRecordException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.HourlyRecordMapper;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.HourlyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HourlyRecordService {

    HourlyRecordRepository hourlyRecordRepository;
    ClinicalDayRepository clinicalDayRepository;
    AuditService auditService;
    DomainAuditEmitter auditEmitter;
    FluidBalanceService fluidBalanceService;
    HourlyRecordMapper hourlyRecordMapper;

    public HourlyRecordResponse getHourlyRecord(UUID id) {
        HourlyRecord record = hourlyRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hourly record not found: " + id));
        return hourlyRecordMapper.toResponse(record);
    }

    public List<HourlyRecordResponse> getHourlyRecordsByClinicalDay(UUID clinicalDayId) {
        return hourlyRecordRepository.findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId)
                .stream().map(hourlyRecordMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public HourlyRecordResponse createHourlyRecord(UUID clinicalDayId, HourlyRecordCreateRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + clinicalDayId));
        assertNotLocked(day);

        int recordHour = request.getRecordTime().getHour();
        if (hourlyRecordRepository.findByClinicalDayIdAndRecordHour(clinicalDayId, recordHour).isPresent()) {
            throw new DuplicateHourlyRecordException(clinicalDayId, recordHour);
        }

        HourlyRecord record = hourlyRecordMapper.toEntity(request);
        record.setClinicalDay(day);
        record.setCreatedBy(userId);
        record.setUpdatedBy(userId);
        autoCalculateMAP(record);
        record = hourlyRecordRepository.save(record);

        boolean backdated = record.getRecordTime() != null
                && record.getRecordTime().getHour() < LocalDateTime.now().getHour();
        if (backdated) {
            log.info("BACK_ENTRY: HourlyRecord {} created for past hour {}", record.getId(), record.getRecordTime());
            auditService.logAction("HourlyRecord", record.getId(), "BACK_ENTRY", userId);
        }

        auditService.logCreate("HourlyRecord", record.getId(), userId);
        final UUID createdRecordId = record.getId();
        auditEmitter.emit("icu", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("hourly-record")
                .action(backdated ? "icu.hourly_record.backdate" : "icu.hourly_record.create")
                .actionType(backdated ? ActionType.BACKDATE : ActionType.CREATE)
                .target(new AuditEvent.AuditTarget("HourlyRecord", createdRecordId.toString(), null))
                .parentTarget(new AuditEvent.AuditTarget("ClinicalDay", clinicalDayId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .changes(List.of(
                        AuditChanges.fieldChanged("vitalFields", DataClass.CLINICAL),
                        AuditChanges.fieldChanged("recordTime", DataClass.IDENTIFIER)))
                .source(AuditEvent.AuditSource.API)
                .build());
        fluidBalanceService.recalculate(clinicalDayId, userId);
        return hourlyRecordMapper.toResponse(record);
    }

    @Transactional
    public HourlyRecordResponse updateHourlyRecord(UUID id, HourlyRecordPatchRequest request, Long userId) {
        HourlyRecord record = hourlyRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hourly record not found: " + id));

        if (!record.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Hourly record was modified by another user");
        }
        assertNotLocked(record.getClinicalDay());

        if (request.getConsciousness() != null) record.setConsciousness(request.getConsciousness());
        if (request.getGcs() != null) record.setGcs(request.getGcs());
        if (request.getTemperature() != null) record.setTemperature(request.getTemperature());
        if (request.getHeartRate() != null) record.setHeartRate(request.getHeartRate());
        if (request.getRespiratoryRate() != null) record.setRespiratoryRate(request.getRespiratoryRate());
        if (request.getSystolicBP() != null) record.setSystolicBP(request.getSystolicBP());
        if (request.getDiastolicBP() != null) record.setDiastolicBP(request.getDiastolicBP());
        if (request.getMeanArterialPressure() != null) record.setMeanArterialPressure(request.getMeanArterialPressure());
        if (request.getSpo2() != null) record.setSpo2(request.getSpo2());
        if (request.getEtco2() != null) record.setEtco2(request.getEtco2());
        if (request.getFio2() != null) record.setFio2(request.getFio2());
        if (request.getCvp() != null) record.setCvp(request.getCvp());
        if (request.getDopamine() != null) record.setDopamine(request.getDopamine());
        if (request.getDobutamine() != null) record.setDobutamine(request.getDobutamine());
        if (request.getNorepinephrine() != null) record.setNorepinephrine(request.getNorepinephrine());
        if (request.getEpinephrine() != null) record.setEpinephrine(request.getEpinephrine());
        if (request.getUrineOutput() != null) record.setUrineOutput(request.getUrineOutput());
        if (request.getDrainOutput() != null) record.setDrainOutput(request.getDrainOutput());
        if (request.getStool() != null) record.setStool(request.getStool());
        if (request.getVomit() != null) record.setVomit(request.getVomit());
        if (request.getPainScore() != null) record.setPainScore(request.getPainScore());
        final boolean notesChanged = request.getNotes() != null;
        if (notesChanged) record.setNotes(request.getNotes());
        autoCalculateMAP(record);
        record.setUpdatedBy(userId);
        record = hourlyRecordRepository.save(record);
        auditService.logUpdate("HourlyRecord", id, userId, null, "Updated hourly record");
        auditEmitter.emit("icu", () -> {
            java.util.List<AuditEvent.AuditChange> changes = new java.util.ArrayList<>();
            if (request.getConsciousness() != null) {
                changes.add(AuditChanges.fieldChanged("consciousness", DataClass.CLINICAL));
            }
            if (request.getGcs() != null) {
                changes.add(AuditChanges.fieldChanged("gcs", DataClass.CLINICAL));
            }
            if (request.getTemperature() != null || request.getHeartRate() != null
                    || request.getRespiratoryRate() != null || request.getSystolicBP() != null
                    || request.getDiastolicBP() != null || request.getMeanArterialPressure() != null
                    || request.getSpo2() != null || request.getEtco2() != null
                    || request.getFio2() != null || request.getCvp() != null) {
                changes.add(AuditChanges.fieldChanged("vitalFields", DataClass.CLINICAL));
            }
            if (request.getDopamine() != null || request.getDobutamine() != null
                    || request.getNorepinephrine() != null || request.getEpinephrine() != null) {
                changes.add(AuditChanges.fieldChanged("vasopressors", DataClass.CLINICAL));
            }
            if (request.getUrineOutput() != null || request.getDrainOutput() != null
                    || request.getStool() != null || request.getVomit() != null
                    || request.getPainScore() != null) {
                changes.add(AuditChanges.fieldChanged("outputFields", DataClass.CLINICAL));
            }
            if (notesChanged) {
                changes.add(AuditChanges.fieldChanged("notes", DataClass.NARRATIVE));
            }
            return AuditEvent.builder()
                    .actor(AuditActorResolver.fromCurrentContext())
                    .eventClass(EventClass.BUSINESS)
                    .module("icu")
                    .functionalArea("hourly-record")
                    .action("icu.hourly_record.update")
                    .actionType(ActionType.UPDATE)
                    .target(new AuditEvent.AuditTarget("HourlyRecord", id.toString(), null))
                    .outcome(AuditEvent.AuditOutcome.SUCCESS)
                    .changes(changes)
                    .source(AuditEvent.AuditSource.API)
                    .build();
        });
        fluidBalanceService.recalculate(record.getClinicalDay().getId(), userId);
        return hourlyRecordMapper.toResponse(record);
    }

    private void autoCalculateMAP(HourlyRecord record) {
        Integer sbp = record.getSystolicBP();
        Integer dbp = record.getDiastolicBP();
        if (sbp != null && dbp != null) {
            int map = (2 * dbp + sbp) / 3;
            record.setMeanArterialPressure(map);
        }
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.NURSE_SIGNED
                || day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }
}
