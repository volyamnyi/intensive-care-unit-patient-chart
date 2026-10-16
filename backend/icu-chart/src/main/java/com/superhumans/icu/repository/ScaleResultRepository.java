package com.superhumans.icu.repository;

import com.superhumans.icu.entity.ScaleResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScaleResultRepository extends JpaRepository<ScaleResult, UUID> {
    // Newest-first ordering (#406): PDF Form 003-15/о and the history UIs
    // pick the first row of these lists — without an explicit ORDER BY the
    // pick was nondeterministic whenever recalculation left several rows.
    List<ScaleResult> findByClinicalDayIdOrderByCalculatedAtDescIdDesc(UUID clinicalDayId);
    List<ScaleResult> findByScaleIdOrderByCalculatedAtDescIdDesc(UUID scaleId);
    List<ScaleResult> findByEpisodeIdOrderByCalculatedAtDescIdDesc(UUID episodeId);
}
