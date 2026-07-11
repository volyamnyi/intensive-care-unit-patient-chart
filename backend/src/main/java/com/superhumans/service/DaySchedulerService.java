package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.IcuCardRepository;
import com.superhumans.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DaySchedulerService {

    private final IcuCardRepository icuCardRepository;
    private final IcuDayService icuDayService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void autoCloseDays() {
        log.info("Running scheduled day close at 07:00");
        List<IcuCard> activeCards = icuCardRepository.findByStatusOrderByCreatedAtDesc(CardStatus.ACTIVE);
        for (IcuCard card : activeCards) {
            List<IcuDay> days = icuDayService.getDaysByCard(card.getId());
            for (IcuDay day : days) {
                if (day.getStatus() == DayStatus.ACTIVE && !day.getDate().equals(LocalDate.now())) {
                    icuDayService.closeDayAndCreateNext(card, day);
                    log.info("Closed day {} for card {}", day.getId(), card.getId());
                }
            }
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void checkEscalations() {
        log.info("Running escalation check at 09:00");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<IcuDay> unsignedDays = icuDayService.getUnsignedDaysBeforeDate(LocalDate.now());

        for (IcuDay day : unsignedDays) {
            if (Boolean.TRUE.equals(day.getEscalationSent())) continue;

            IcuCard card = day.getIcuCard();
            List<User> heads = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.HEAD_OF_DEPARTMENT)
                    .toList();

            for (User head : heads) {
                emailService.sendEscalation(head.getEmail(),
                        card.getPatientName(),
                        day.getDayNumber(),
                        day.getDate());
            }
            day.setEscalationSent(true);
            log.info("Escalation sent for day {} card {}", day.getId(), card.getId());
        }
    }
}
