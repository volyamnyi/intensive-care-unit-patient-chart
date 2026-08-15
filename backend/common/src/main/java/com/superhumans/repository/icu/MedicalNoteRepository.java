package com.superhumans.repository.icu;

import com.superhumans.entity.MedicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MedicalNoteRepository extends JpaRepository<MedicalNote, UUID> {
    List<MedicalNote> findByClinicalDayIdOrderByCreatedAtAsc(UUID clinicalDayId);
    List<MedicalNote> findByClinicalDayIdAndRoleOrderByCreatedAtAsc(UUID clinicalDayId, String role);
    List<MedicalNote> findByClinicalDayIdAndNoteTypeOrderByCreatedAtAsc(UUID clinicalDayId, String noteType);
    long countByClinicalDayId(UUID clinicalDayId);
}
