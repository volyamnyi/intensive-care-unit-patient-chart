package com.superhumans.audit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Single facade for Audit v2 operational metrics (F8, mechanism D6).
 * Every counter carries a {@code module} tag (platform/icu/medication/
 * prosthetics/legacy); alerting thresholds live in the runbook. Scraped
 * via the Actuator {@code metrics} endpoint (same auth as the API).
 */
@Component
@RequiredArgsConstructor
public class AuditMetrics {

    private final MeterRegistry meterRegistry;

    public void emitted(String module) {
        counter("audit.events.emitted", module).increment();
    }

    public void stored(String module) {
        counter("audit.events.stored", module).increment();
    }

    public void duplicate(String module) {
        counter("audit.events.duplicate", module).increment();
    }

    public void failed(String module, String reason) {
        Counter.builder("audit.events.failed")
                .tag("module", module)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void backfilled(int rows) {
        Counter.builder("audit.backfill.rows").register(meterRegistry).increment(rows);
    }

    public Timer.Sample searchTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopSearch(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("audit.query.duration")
                .tag("operation", operation)
                .register(meterRegistry));
    }

    private Counter counter(String name, String module) {
        return Counter.builder(name).tag("module", module).register(meterRegistry);
    }
}
