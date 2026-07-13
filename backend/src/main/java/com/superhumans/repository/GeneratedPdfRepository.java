package com.superhumans.repository;

import com.superhumans.entity.GeneratedPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeneratedPdfRepository extends JpaRepository<GeneratedPdf, UUID> {
    Optional<GeneratedPdf> findFirstByClinicalDayIdOrderByFileVersionDesc(UUID clinicalDayId);
    List<GeneratedPdf> findByClinicalDayIdOrderByFileVersionAsc(UUID clinicalDayId);
    long countByClinicalDayId(UUID clinicalDayId);
}
