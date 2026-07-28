package com.superhumans.medicationsheet.service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.entity.VitalSignDay;
import com.superhumans.medicationsheet.entity.VitalSignEntry;
import com.superhumans.medicationsheet.entity.VitalSignList;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import com.superhumans.medicationsheet.repository.VitalSignDayRepository;
import com.superhumans.medicationsheet.repository.VitalSignEntryRepository;
import com.superhumans.medicationsheet.repository.VitalSignListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VitalSignService {

    private final VitalSignListRepository vitalListRepository;
    private final VitalSignDayRepository vitalDayRepository;
    private final VitalSignEntryRepository vitalEntryRepository;
    private final PrescriptionListRepository listRepository;

    @Transactional
    public VitalSignList getOrCreate(UUID prescriptionListId) {
        return vitalListRepository.findByPrescriptionListId(prescriptionListId)
                .orElseGet(() -> {
                    PrescriptionList list = listRepository.findById(prescriptionListId)
                            .orElseThrow();
                    VitalSignList vitalList = VitalSignList.builder()
                            .prescriptionList(list)
                            .build();
                    vitalList.setCreatedBy(0L);
                    vitalList.setUpdatedBy(0L);
                    vitalList = vitalListRepository.save(vitalList);

                    LocalDate start = LocalDate.now();
                    for (int i = 0; i < 21; i++) {
                        VitalSignDay day = VitalSignDay.builder()
                                .vitalList(vitalList)
                                .dayDate(start.plusDays(i))
                                .build();
                        day.setCreatedBy(0L);
                        day.setUpdatedBy(0L);
                        day = vitalDayRepository.save(day);

                        for (String period : List.of("morning", "day", "evening", "night")) {
                            VitalSignEntry entry = VitalSignEntry.builder()
                                    .day(day)
                                    .period(period)
                                    .build();
                            entry.setCreatedBy(0L);
                            entry.setUpdatedBy(0L);
                            vitalEntryRepository.save(entry);
                        }
                    }
                    return vitalList;
                });
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
        return vitalEntryRepository.save(entry);
    }

    @Transactional
    public VitalSignEntry getOrCreateEntry(UUID dayId, String period) {
        return vitalEntryRepository.findByDayIdAndPeriod(dayId, period)
                .orElseGet(() -> {
                    VitalSignDay day = vitalDayRepository.findById(dayId).orElseThrow();
                    VitalSignEntry entry = VitalSignEntry.builder()
                            .day(day)
                            .period(period)
                            .build();
                    entry.setCreatedBy(0L);
                    entry.setUpdatedBy(0L);
                    return vitalEntryRepository.save(entry);
                });
    }

    @Transactional
    public VitalSignEntry saveNextEntry(UUID prescriptionListId, VitalSignEntry update) {
        VitalSignList list = getOrCreate(prescriptionListId);
        List<VitalSignDay> days = getDays(list.getId());
        for (VitalSignDay day : days) {
            List<VitalSignEntry> entries = getEntries(day.getId());
            for (VitalSignEntry entry : entries) {
                if (entry.getTemperature() == null && entry.getSystolicBp() == null
                        && entry.getDiastolicBp() == null && entry.getSpo2() == null
                        && entry.getPulse() == null && entry.getStool() == null
                        && entry.getPainScore() == null) {
                    return updateEntry(entry.getId(), update);
                }
            }
        }
        throw new IllegalStateException("No empty vital sign entry slot available");
    }
}
