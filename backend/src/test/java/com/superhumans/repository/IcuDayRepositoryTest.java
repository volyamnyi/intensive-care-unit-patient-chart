package com.superhumans.repository;

import com.superhumans.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IcuDayRepositoryTest {

    @Autowired private IcuCardRepository icuCardRepository;
    @Autowired private IcuDayRepository icuDayRepository;

    private IcuCard createCard() {
        IcuCard card = IcuCard.builder()
                .patientId(100L).patientName("Day Test")
                .medicalCardNumber("DAY-001")
                .admissionDate(LocalDateTime.now())
                .diagnosis("Test")
                .status(CardStatus.ACTIVE)
                .createdBy("test").createdAt(LocalDateTime.now())
                .build();
        return icuCardRepository.save(card);
    }

    @Test
    void saveDay_shouldPersistAllFields() {
        IcuCard card = createCard();
        IcuDay day = IcuDay.builder()
                .icuCard(card)
                .dayNumber(1)
                .date(LocalDate.now())
                .status(DayStatus.ACTIVE)
                .doctorId(1L)
                .build();

        IcuDay saved = icuDayRepository.save(day);

        assertNotNull(saved.getId());
        assertEquals(1, saved.getDayNumber());
        assertEquals(DayStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void findByCardId_shouldReturnDaysOrdered() {
        IcuCard card = createCard();
        IcuDay day1 = IcuDay.builder().icuCard(card).dayNumber(1).date(LocalDate.now()).status(DayStatus.ACTIVE).build();
        IcuDay day2 = IcuDay.builder().icuCard(card).dayNumber(2).date(LocalDate.now().plusDays(1)).status(DayStatus.ACTIVE).build();
        icuDayRepository.save(day1);
        icuDayRepository.save(day2);

        List<IcuDay> days = icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(card.getId());

        assertEquals(2, days.size());
        assertEquals(1, days.get(0).getDayNumber());
        assertEquals(2, days.get(1).getDayNumber());
    }

    @Test
    void findByCardAndDate_shouldReturnOptional() {
        IcuCard card = createCard();
        IcuDay day = IcuDay.builder().icuCard(card).dayNumber(1).date(LocalDate.now()).status(DayStatus.ACTIVE).build();
        icuDayRepository.save(day);

        var found = icuDayRepository.findByIcuCardIdAndDate(card.getId(), LocalDate.now());

        assertTrue(found.isPresent());
        assertEquals(1, found.get().getDayNumber());
    }
}
