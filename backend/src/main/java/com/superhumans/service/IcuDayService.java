package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.IcuDayRepository;
import com.superhumans.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IcuDayService {

    private final IcuDayRepository icuDayRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final FluidBalanceService fluidBalanceService;
    private final AuditService auditService;

    public List<IcuDay> getDaysByCard(Long icuCardId) {
        return icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(icuCardId);
    }

    public IcuDay getDay(Long id) {
        return icuDayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Day not found: " + id));
    }

    @Transactional
    public IcuDay signOff(Long dayId, Long doctorId, String doctorLogin) {
        IcuDay day = getDay(dayId);
        if (day.getStatus() != DayStatus.ACTIVE) {
            throw new RuntimeException("Day is not active");
        }
        day.setStatus(DayStatus.SIGNED);
        day.setDoctorId(doctorId);
        day.setSignedAt(java.time.LocalDateTime.now());

        fluidBalanceService.calculateAndSave(day);
        auditService.log(doctorLogin, "SIGN_OFF_DAY", "IcuDay", dayId,
                "dayNumber=" + day.getDayNumber(), null);
        return icuDayRepository.save(day);
    }

    @Transactional
    public void closeDayAndCreateNext(IcuCard card, IcuDay currentDay) {
        currentDay.setStatus(DayStatus.ARCHIVED);
        icuDayRepository.save(currentDay);

        IcuDay nextDay = IcuDay.builder()
                .icuCard(card)
                .dayNumber(currentDay.getDayNumber() + 1)
                .date(currentDay.getDate().plusDays(1))
                .status(DayStatus.ACTIVE)
                .build();
        icuDayRepository.save(nextDay);

        List<Prescription> activePrescriptions = prescriptionRepository
                .findByIcuCardIdAndStatus(card.getId(), PrescriptionStatus.ACTIVE);
        for (Prescription p : activePrescriptions) {
            p.setStartDate(nextDay.getDate());
        }
        prescriptionRepository.saveAll(activePrescriptions);
    }

    public List<IcuDay> getUnsignedDaysBeforeDate(LocalDate date) {
        return icuDayRepository.findByStatusAndDateBefore(DayStatus.ACTIVE, date);
    }
}
