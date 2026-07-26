package com.superhumans.repository;

import com.superhumans.entity.Episode;
import com.superhumans.entity.EpisodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EpisodeRepository extends JpaRepository<Episode, UUID> {
    List<Episode> findByPatientId(Long patientId);
    Optional<Episode> findByPatientIdAndStatus(Long patientId, EpisodeStatus status);
    List<Episode> findByStatus(EpisodeStatus status);
    List<Episode> findByDepartmentId(UUID departmentId);
    List<Episode> findByAdmissionDateBetween(LocalDateTime start, LocalDateTime end);
    long countByStatus(EpisodeStatus status);

    @Query("SELECT e FROM Episode e WHERE e.status = 'ACTIVE' ORDER BY e.createdAt DESC")
    List<Episode> findAllActive();
}
