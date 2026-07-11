package com.superhumans.repository;

import com.superhumans.entity.CardStatus;
import com.superhumans.entity.IcuCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IcuCardRepository extends JpaRepository<IcuCard, Long> {
    List<IcuCard> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<IcuCard> findByStatusOrderByCreatedAtDesc(CardStatus status);
}
