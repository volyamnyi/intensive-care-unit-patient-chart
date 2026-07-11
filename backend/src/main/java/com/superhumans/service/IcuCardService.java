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
                              String diagnosis, Integer apacheIi, Integer sofa, String createdBy,
                              Integer patientHeight, Integer patientWeight, String bloodGroup,
                              String rhFactor, String patientSexCode, java.time.LocalDate patientBirthDate) {
        Integer idealBodyWeight = calculateIdealBodyWeight(patientHeight, patientSexCode);

        IcuCard card = IcuCard.builder()
                .patientId(patientId)
                .patientName(patientName)
                .medicalCardNumber(medicalCardNumber)
                .admissionDate(LocalDateTime.now())
                .diagnosis(diagnosis)
                .apacheIi(apacheIi)
                .sofa(sofa)
                .patientHeight(patientHeight)
                .patientWeight(patientWeight)
                .idealBodyWeight(idealBodyWeight)
                .bloodGroup(bloodGroup)
                .rhFactor(rhFactor)
                .patientSexCode(patientSexCode)
                .patientBirthDate(patientBirthDate)
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
                .apacheIi(apacheIi)
                .sofa(sofa)
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

    public static Integer calculateIdealBodyWeight(Integer heightCm, String sexCode) {
        if (heightCm == null || heightCm <= 0) return null;
        double heightInInches = heightCm / 2.54;
        if (heightInInches <= 60) heightInInches = 60;
        if ("F".equalsIgnoreCase(sexCode) || "W".equalsIgnoreCase(sexCode)) {
            return (int) Math.round(45.5 + 2.3 * (heightInInches - 60));
        } else {
            return (int) Math.round(50.0 + 2.3 * (heightInInches - 60));
        }
    }
}
