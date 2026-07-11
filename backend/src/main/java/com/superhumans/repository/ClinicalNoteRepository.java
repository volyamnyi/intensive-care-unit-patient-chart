package com.superhumans.repository;

import com.superhumans.entity.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {
    List<ClinicalNote> findByIcuDayIdOrderByCreatedAtDesc(Long icuDayId);
}
