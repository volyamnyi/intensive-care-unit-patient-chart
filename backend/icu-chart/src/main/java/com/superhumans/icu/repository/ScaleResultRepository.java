package com.superhumans.icu.repository;

import com.superhumans.icu.entity.ScaleResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface ScaleResultRepository extends JpaRepository<ScaleResult, UUID> {
    // Newest-first ordering (#406): PDF Form 003-15/о and the history UIs
    // pick the first row of these lists — without an explicit ORDER BY the
    // pick was nondeterministic whenever recalculation left several rows.
    List<ScaleResult> findByClinicalDayIdOrderByCalculatedAtDescIdDesc(UUID clinicalDayId);
    List<ScaleResult> findByScaleIdOrderByCalculatedAtDescIdDesc(UUID scaleId);
    List<ScaleResult> findByEpisodeIdOrderByCalculatedAtDescIdDesc(UUID episodeId);

    /**
     * Same rows as {@link #findByClinicalDayIdOrderByCalculatedAtDescIdDesc},
     * but with the scale fetched in the same query.
     *
     * <p>{@code ScaleResult.scale} is lazy. The PDF render runs outside a
     * transaction, so reading {@code scale.getName()} off the detached proxy
     * threw {@code LazyInitializationException} and turned the whole
     * generation into a 500. The production seed has scale rows for the
     * clinical days the E2E PDF spec uses, so only that environment hit it.
     */
    @Query("select sr from ScaleResult sr left join fetch sr.scale "
            + "where sr.clinicalDay.id = :clinicalDayId "
            + "order by sr.calculatedAt desc, sr.id desc")
    List<ScaleResult> findByClinicalDayIdWithScale(UUID clinicalDayId);

    /** Episode-level counterpart of {@link #findByClinicalDayIdWithScale}. */
    @Query("select sr from ScaleResult sr left join fetch sr.scale "
            + "where sr.episodeId = :episodeId "
            + "order by sr.calculatedAt desc, sr.id desc")
    List<ScaleResult> findByEpisodeIdWithScale(UUID episodeId);
}
