package com.superhumans.repository;

import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicalDayRepository extends JpaRepository<ClinicalDay, UUID> {
    List<ClinicalDay> findByEpisodeIdOrderByDayNumberAsc(UUID episodeId);
    Optional<ClinicalDay> findByEpisodeIdAndStatus(UUID episodeId, ClinicalDayStatus status);
    Optional<ClinicalDay> findFirstByEpisodeIdOrderByDayNumberDesc(UUID episodeId);
    List<ClinicalDay> findByEpisodeIdAndStatusInOrderByDayNumberAsc(UUID episodeId, List<ClinicalDayStatus> statuses);
    long countByEpisodeId(UUID episodeId);
}
