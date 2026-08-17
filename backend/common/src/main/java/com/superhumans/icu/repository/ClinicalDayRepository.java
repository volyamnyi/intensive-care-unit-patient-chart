package com.superhumans.icu.repository;

import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.ClinicalDayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicalDayRepository extends JpaRepository<ClinicalDay, UUID> {
    List<ClinicalDay> findByEpisodeIdOrderByDayNumberAsc(UUID episodeId);
    Optional<ClinicalDay> findByEpisodeIdAndStatus(UUID episodeId, ClinicalDayStatus status);
    Optional<ClinicalDay> findFirstByEpisodeIdOrderByDayNumberDesc(UUID episodeId);
    List<ClinicalDay> findByEpisodeIdAndStatusInOrderByDayNumberAsc(UUID episodeId, List<ClinicalDayStatus> statuses);
    List<ClinicalDay> findByStatusIn(List<ClinicalDayStatus> statuses);
    long countByEpisodeId(UUID episodeId);
    long countByStatus(ClinicalDayStatus status);

    @Query("SELECT cd FROM ClinicalDay cd WHERE cd.episode.id = :episodeId AND (cd.status = 'OPEN' OR cd.status = 'REOPENED')")
    Optional<ClinicalDay> findCurrentDayByEpisodeId(@Param("episodeId") UUID episodeId);

    @Query("SELECT cd FROM ClinicalDay cd WHERE cd.endDateTime <= :now AND (cd.status = 'OPEN' OR cd.status = 'REOPENED')")
    List<ClinicalDay> findDaysToAutoClose(@Param("now") LocalDateTime now);
}
