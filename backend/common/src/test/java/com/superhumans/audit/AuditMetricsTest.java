package com.superhumans.audit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditMetricsTest {

    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final AuditMetrics metrics = new AuditMetrics(registry);

    private double counter(String name, String... tags) {
        return registry.get(name).tags(tags).counter().count();
    }

    @Test
    void emittedStoredDuplicateFailed_countByModule() {
        metrics.emitted("icu");
        metrics.emitted("icu");
        metrics.stored("icu");
        metrics.duplicate("icu");
        metrics.failed("platform", "RuntimeException");

        assertThat(counter("audit.events.emitted", "module", "icu")).isEqualTo(2.0);
        assertThat(counter("audit.events.stored", "module", "icu")).isEqualTo(1.0);
        assertThat(counter("audit.events.duplicate", "module", "icu")).isEqualTo(1.0);
        assertThat(registry.get("audit.events.failed")
                .tags("module", "platform", "reason", "RuntimeException").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void searchTimer_recordsOperationLatency() {
        var sample = metrics.searchTimer();
        metrics.stopSearch(sample, "search");

        assertThat(registry.get("audit.query.duration")
                .tag("operation", "search").timer().count()).isEqualTo(1L);
    }

    @Test
    void backfilled_countsRows() {
        metrics.backfilled(7);

        assertThat(registry.get("audit.backfill.rows").counter().count()).isEqualTo(7.0);
    }
}
