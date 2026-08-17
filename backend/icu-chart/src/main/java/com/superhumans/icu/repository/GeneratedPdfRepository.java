package com.superhumans.icu.repository;

import com.superhumans.icu.entity.GeneratedPdf;
import com.superhumans.icu.entity.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeneratedPdfRepository extends JpaRepository<GeneratedPdf, UUID> {
    Optional<GeneratedPdf> findFirstByClinicalDayIdOrderByFileVersionDesc(UUID clinicalDayId);
    List<GeneratedPdf> findByClinicalDayIdOrderByFileVersionAsc(UUID clinicalDayId);
    long countByClinicalDayId(UUID clinicalDayId);
    List<GeneratedPdf> findByTransferStatus(TransferStatus transferStatus);
}
