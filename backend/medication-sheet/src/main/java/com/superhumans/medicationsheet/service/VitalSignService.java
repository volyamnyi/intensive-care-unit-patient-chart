package com.superhumans.medicationsheet.service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditChanges;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.medicationsheet.entity.VitalSignDay;
import com.superhumans.medicationsheet.entity.VitalSignEntry;
import com.superhumans.medicationsheet.entity.VitalSignList;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import com.superhumans.medicationsheet.repository.VitalSignDayRepository;
import com.superhumans.medicationsheet.repository.VitalSignEntryRepository;
import com.superhumans.medicationsheet.repository.VitalSignListRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VitalSignService {

    VitalSignListRepository vitalListRepository;
    VitalSignDayRepository vitalDayRepository;
    VitalSignEntryRepository vitalEntryRepository;
    PrescriptionListRepository listRepository;
    DomainAuditEmitter auditEmitter;
    org.springframework.transaction.support.TransactionTemplate provisioningTx;

    public VitalSignService(VitalSignListRepository vitalListRepository,
            VitalSignDayRepository vitalDayRepository,
            VitalSignEntryRepository vitalEntryRepository,
            PrescriptionListRepository listRepository,
            DomainAuditEmitter auditEmitter,
            @org.springframework.beans.factory.annotation.Qualifier("medTransactionManager")
            org.springframework.transaction.PlatformTransactionManager medTransactionManager) {
        this.vitalListRepository = vitalListRepository;
        this.vitalDayRepository = vitalDayRepository;
        this.vitalEntryRepository = vitalEntryRepository;
        this.listRepository = listRepository;
        this.auditEmitter = auditEmitter;
        // #382: provisioning needs ONE med-leg transaction - the chained primary
        // manager would open all four pools for a med-only write. REQUIRES_NEW on
        // the chained manager is a documented trap (#375): the definition is
        // forwarded to every leg. The med manager directly is the safe shape.
        this.provisioningTx = new org.springframework.transaction.support.TransactionTemplate(
                medTransactionManager);
    }

    /**
     * #365: the read view of the vitals grid. Never provisions - a GET that
     * wrote 107 rows was a CSRF surface (SameSite=Lax rides top-level GET
     * navigations), so provisioning moved behind the explicit POST ensure.
     * Returns the existing grid or an empty one; callers that need the grid to
     * exist call {@link #getOrCreate(UUID)} through the POST endpoint.
     */
    @Transactional(readOnly = true)
    public Optional<VitalSignList> findList(UUID prescriptionListId) {
        return vitalListRepository.findByPrescriptionListId(prescriptionListId);
    }

    // No @Transactional here (#382): the convergence catch must run OUTSIDE the
    // provisioning transaction. After the unique-index violation aborts the
    // Postgres transaction, no statement can run inside it - including the
    // re-query. Each repository call opens its own short transaction, so the
    // loser re-reads the winner's committed grid in a fresh one.
    public VitalSignList getOrCreate(UUID prescriptionListId) {
        return vitalListRepository.findByPrescriptionListId(prescriptionListId)
                .orElseGet(() -> {
                    try {
                        return provisioningTx.execute(status -> createGrid(prescriptionListId));
                    } catch (org.springframework.dao.DataIntegrityViolationException lostTheRace) {
                        // #382: another first-load created this grid between the
                        // find and the insert; the unique index turned the race
                        // into this exception. Converge on the winner's grid.
                        return vitalListRepository.findByPrescriptionListId(prescriptionListId)
                                .orElseThrow(() -> lostTheRace);
                    }
                });
    }

    // Plain private body: the transaction is owned by the TransactionTemplate in
    // getOrCreate - never annotate this, the convergence depends on the caller
    // sitting outside the transaction.
    private VitalSignList createGrid(UUID prescriptionListId) {
                    PrescriptionList list = listRepository.findById(prescriptionListId)
                            .orElseThrow(() -> new com.superhumans.exception.NotFoundException(
                                    "Prescription list not found: " + prescriptionListId));
                    VitalSignList vitalList = VitalSignList.builder()
                            .prescriptionList(list)
                            .build();
                    vitalList.setCreatedBy(0L);
                    vitalList.setUpdatedBy(0L);
                    vitalList = vitalListRepository.save(vitalList);

// Batched (#429 item 3): 21 day + 84 entry inserts in two statements
                        // instead of 105 round-trips. Audit fields are set explicitly
                        // because the builder does not apply FieldDefaults.
                        LocalDate start = LocalDate.now();
                        List<VitalSignDay> days = new ArrayList<>(21);
                        for (int i = 0; i < 21; i++) {
                            VitalSignDay day = VitalSignDay.builder()
                                    .vitalList(vitalList)
                                    .dayDate(start.plusDays(i))
                                    .build();
                            day.setCreatedBy(0L);
                            day.setUpdatedBy(0L);
                            days.add(day);
                        }
                        List<VitalSignDay> persistedDays = vitalDayRepository.saveAll(days);

                        List<VitalSignEntry> entries = new ArrayList<>(84);
                        for (VitalSignDay day : persistedDays) {
                            for (String period : List.of("morning", "day", "evening", "night")) {
                                VitalSignEntry entry = VitalSignEntry.builder()
                                        .day(day)
                                        .period(period)
                                        .build();
                                entry.setCreatedBy(0L);
                                entry.setUpdatedBy(0L);
                                entries.add(entry);
                            }
                        }
                        vitalEntryRepository.saveAll(entries);
                    final UUID initializedListId = vitalList.getId();
                    auditEmitter.emit("medication", () -> AuditEvent.builder()
                            .actor(AuditActorResolver.fromCurrentContextOrSystem("vital-sign-job"))
                            .eventClass(EventClass.BUSINESS)
                            .module("medication")
                            .functionalArea("vitals")
                            .action("medication.vitals.grid.initialize")
                            .actionType(ActionType.GRID_INITIALIZE)
                            .target(new AuditEvent.AuditTarget(
                                    "VitalSignList", initializedListId.toString(), null))
                            .parentTarget(new AuditEvent.AuditTarget(
                                    "PrescriptionList", prescriptionListId.toString(), null))
                            .outcome(AuditEvent.AuditOutcome.SUCCESS)
                            .changes(List.of(
                                    AuditChanges.fieldChanged("vitalGrid", DataClass.CLINICAL)))
                            .affectedRecords(106)
                            .source(AuditEvent.AuditSource.API)
                            .build());
                    return vitalList;
    }

    @Transactional(readOnly = true)
    public List<VitalSignDay> getDays(UUID vitalListId) {
        return vitalDayRepository.findByVitalListIdOrderByDayDateAsc(vitalListId);
    }

    @Transactional(readOnly = true)
    public List<VitalSignEntry> getEntries(UUID dayId) {
        return vitalEntryRepository.findByDayId(dayId);
    }

    @Transactional
    public VitalSignEntry updateEntry(UUID entryId, VitalSignEntry update) {
        VitalSignEntry entry = vitalEntryRepository.findById(entryId).orElseThrow();
        if (update.getTemperature() != null) entry.setTemperature(update.getTemperature());
        if (update.getSystolicBp() != null) entry.setSystolicBp(update.getSystolicBp());
        if (update.getDiastolicBp() != null) entry.setDiastolicBp(update.getDiastolicBp());
        if (update.getSpo2() != null) entry.setSpo2(update.getSpo2());
        if (update.getPulse() != null) entry.setPulse(update.getPulse());
        if (update.getStool() != null) entry.setStool(update.getStool());
        if (update.getPainScore() != null) entry.setPainScore(update.getPainScore());
        entry.setUpdatedBy(0L);
        VitalSignEntry saved = vitalEntryRepository.save(entry);
        final UUID updatedEntryId = saved.getId();
        auditEmitter.emit("medication", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.BUSINESS)
                .module("medication")
                .functionalArea("vitals")
                .action("medication.vitals.entry.update")
                .actionType(ActionType.UPDATE)
                .target(new AuditEvent.AuditTarget("VitalSignEntry", updatedEntryId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .changes(List.of(AuditChanges.fieldChanged("vitalFields", DataClass.CLINICAL)))
                .source(AuditEvent.AuditSource.API)
                .build());
        return saved;
    }

// Same shape as getOrCreate (#382): catch outside the aborted transaction.
public VitalSignEntry getOrCreateEntry(UUID dayId, String period) {
        return vitalEntryRepository.findByDayIdAndPeriod(dayId, period)
                .orElseGet(() -> {
                    try {
                        return provisioningTx.execute(status -> createEntry(dayId, period));
                    } catch (org.springframework.dao.DataIntegrityViolationException lostTheRace) {
                        // The day/period slot was taken between the find and the
                        // insert; converge on the winner's entry.
                        return vitalEntryRepository.findByDayIdAndPeriod(dayId, period)
                                .orElseThrow(() -> lostTheRace);
                    }
                });
    }

    private VitalSignEntry createEntry(UUID dayId, String period) {
                    VitalSignDay day = vitalDayRepository.findById(dayId).orElseThrow();
                    VitalSignEntry entry = VitalSignEntry.builder()
                            .day(day)
                            .period(period)
                            .build();
                    entry.setCreatedBy(0L);
                    entry.setUpdatedBy(0L);
                    VitalSignEntry saved = vitalEntryRepository.save(entry);
                    final UUID createdEntryId = saved.getId();
                    auditEmitter.emit("medication", () -> AuditEvent.builder()
                            .actor(AuditActorResolver.fromCurrentContext())
                            .eventClass(EventClass.BUSINESS)
                            .module("medication")
                            .functionalArea("vitals")
                            .action("medication.vitals.entry.create")
                            .actionType(ActionType.CREATE)
                            .target(new AuditEvent.AuditTarget(
                                    "VitalSignEntry", createdEntryId.toString(), null))
                            .parentTarget(new AuditEvent.AuditTarget("VitalSignDay", dayId.toString(), null))
                            .outcome(AuditEvent.AuditOutcome.SUCCESS)
                            .changes(List.of(
                                    AuditChanges.fieldChanged("vitalFields", DataClass.CLINICAL)))
                            .source(AuditEvent.AuditSource.API)
                            .build());
                    return saved;
    }

    /**
     * Idempotent create/update of the (dayId, period) slot (#406): the entry
     * is created when absent, then the submitted values are written in place.
     * A double submit lands on the same row instead of the next empty slot.
     */
    @Transactional
    public VitalSignEntry saveEntry(UUID dayId, String period, VitalSignEntry update) {
        VitalSignEntry entry = getOrCreateEntry(dayId, period);
        return updateEntry(entry.getId(), update);
    }
}
