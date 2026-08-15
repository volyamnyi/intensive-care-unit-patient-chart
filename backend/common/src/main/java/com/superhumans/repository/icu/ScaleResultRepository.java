package com.superhumans.repository.icu;

import com.superhumans.entity.ScaleResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScaleResultRepository extends JpaRepository<ScaleResult, UUID> {
    List<ScaleResult> findByClinicalDayId(UUID clinicalDayId);
    Optional<ScaleResult> findByClinicalDayIdAndScaleId(UUID clinicalDayId, UUID scaleId);
    List<ScaleResult> findByScaleId(UUID scaleId);
    List<ScaleResult> findByEpisodeId(UUID episodeId);
    Optional<ScaleResult> findByEpisodeIdAndScaleId(UUID episodeId, UUID scaleId);
}
