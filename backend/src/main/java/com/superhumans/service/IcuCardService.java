package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.IcuCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IcuCardService {

    private final IcuCardRepository icuCardRepository;
    private final AuditService auditService;

    @Transactional
    public IcuCard createCard(Long patientId, String patientName, String medicalCardNumber,
                              String diagnosis, Integer apacheIi, Integer sofa, String createdBy) {
        IcuCard card = IcuCard.builder()
                .patientId(patientId)
                .patientName(patientName)
                .medicalCardNumber(medicalCardNumber)
                .admissionDate(LocalDateTime.now())
                .diagnosis(diagnosis)
                .apacheIi(apacheIi)
                .sofa(sofa)
                .status(CardStatus.ACTIVE)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        IcuCard saved = icuCardRepository.save(card);

        IcuDay firstDay = IcuDay.builder()
                .icuCard(saved)
                .dayNumber(1)
                .date(LocalDate.now())
                .status(DayStatus.ACTIVE)
                .build();
        saved.getIcuDays().add(firstDay);

        auditService.log(createdBy, "CREATE_CARD", "IcuCard", saved.getId(), null, null);
        return icuCardRepository.save(saved);
    }

    public IcuCard getCard(Long id) {
        return icuCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found: " + id));
    }

    public List<IcuCard> getCardsByPatient(Long patientId) {
        return icuCardRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    public List<IcuCard> getActiveCards() {
        return icuCardRepository.findByStatusOrderByCreatedAtDesc(CardStatus.ACTIVE);
    }
}
