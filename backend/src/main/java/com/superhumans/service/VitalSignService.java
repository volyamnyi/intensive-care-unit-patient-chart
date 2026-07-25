package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.*;
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
public class VitalSignService {

    private final VitalSignListRepository vitalListRepository;
    private final VitalSignDayRepository vitalDayRepository;
    private final VitalSignEntryRepository vitalEntryRepository;
    private final PrescriptionListRepository listRepository;

    @Transactional
    public VitalSignList getOrCreate(UUID prescriptionListId, Long userId) {
        return vitalListRepository.findByPrescriptionListId(prescriptionListId)
                .orElseGet(() -> {
                    PrescriptionList list = listRepository.findById(prescriptionListId)
                            .orElseThrow();
                    VitalSignList vitalList = VitalSignList.builder()
                            .prescriptionList(list)
                            .build();
                    vitalList.setCreatedBy(userId);
                    vitalList.setUpdatedBy(userId);
                    vitalList = vitalListRepository.save(vitalList);

                    LocalDate start = LocalDate.now();
                    for (int i = 0; i < 21; i++) {
                        VitalSignDay day = VitalSignDay.builder()
                                .vitalList(vitalList)
                                .dayDate(start.plusDays(i))
                                .build();
                        day.setCreatedBy(userId);
                        day.setUpdatedBy(userId);
                        day = vitalDayRepository.save(day);

                        for (String period : List.of("morning", "evening")) {
                            VitalSignEntry entry = VitalSignEntry.builder()
                                    .day(day)
                                    .period(period)
                                    .build();
                            entry.setCreatedBy(userId);
                            entry.setUpdatedBy(userId);
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
    public VitalSignEntry updateEntry(UUID entryId, VitalSignEntry update, Long userId) {
        VitalSignEntry entry = vitalEntryRepository.findById(entryId).orElseThrow();
        entry.setTemperature(update.getTemperature());
        entry.setSystolicBp(update.getSystolicBp());
        entry.setDiastolicBp(update.getDiastolicBp());
        entry.setSpo2(update.getSpo2());
        entry.setPulse(update.getPulse());
        entry.setStool(update.getStool());
        entry.setPainScore(update.getPainScore());
        entry.setUpdatedBy(userId);
        return vitalEntryRepository.save(entry);
    }
}
