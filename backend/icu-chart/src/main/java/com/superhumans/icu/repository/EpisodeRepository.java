package com.superhumans.icu.repository;

import com.superhumans.icu.entity.Episode;
import com.superhumans.icu.entity.EpisodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EpisodeRepository extends JpaRepository<Episode, UUID> {
    // Admission-date order with id tiebreaker (#411 item 4): identical
    // requests must return identical row order. The unused unfiltered
    // finders are dropped (unbounded full-table dumps stay out of the API).
    List<Episode> findByPatientIdOrderByAdmissionDateDescIdDesc(Long patientId);
    Optional<Episode> findByPatientIdAndStatus(Long patientId, EpisodeStatus status);
    List<Episode> findByStatusOrderByAdmissionDateDescIdDesc(EpisodeStatus status);
    long countByStatus(EpisodeStatus status);

    long countByDepartmentIdAndStatus(UUID departmentId, EpisodeStatus status);

    @Query("SELECT e FROM Episode e WHERE e.status = 'ACTIVE' ORDER BY e.createdAt DESC")
    List<Episode> findAllActive();

    @Query("SELECT e FROM Episode e WHERE e.departmentId = :departmentId AND e.status = 'ACTIVE' ORDER BY e.createdAt DESC")
    List<Episode> findAllActiveByDepartmentId(@Param("departmentId") UUID departmentId);
}
