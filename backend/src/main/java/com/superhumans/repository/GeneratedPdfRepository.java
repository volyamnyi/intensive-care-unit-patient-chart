package com.superhumans.repository;

import com.superhumans.entity.GeneratedPdf;
import com.superhumans.entity.TransferStatus;
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
