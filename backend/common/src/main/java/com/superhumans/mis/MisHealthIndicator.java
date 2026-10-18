package com.superhumans.mis;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * MIS reachability signal for {@code /actuator/health} (#412 item 7).
 *
 * <p>Before this indicator the whole application stayed UP while MIS was
 * completely down: patient search, prescription catalogs, prosthetics
 * eligibility and every generated PDF silently degraded to local fallbacks.
 * Actuator exposure itself is restricted to ADMINISTRATOR/AUDITOR (#370 item
 * 4), so the detail carries the MIS base URL but never patient data.
 *
 * <p>State is intentionally cheap and lock-free: a timestamp and the last
 * error class. Health is DOWN only when the last {@link #recordFailure} is
 * newer than the configured window — a stale failure never keeps the service
 * DOWN forever without a probe.
 */
@Slf4j
@Component("mis")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MisHealthIndicator implements HealthIndicator {

    /** A failure older than this is considered recovered-by-absence. */
    static final Duration FAILURE_WINDOW = Duration.ofMinutes(5);

    private final MisApiProperties properties;
    private final Clock clock;
    private final AtomicReference<String> lastError = new AtomicReference<>();
    private final AtomicReference<Instant> lastFailureAt = new AtomicReference<>();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();

    @org.springframework.beans.factory.annotation.Autowired
    public MisHealthIndicator(MisApiProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /** Test seam: a fixed clock makes the failure window deterministic. */
    MisHealthIndicator(MisApiProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    void recordSuccess() {
        successCount.incrementAndGet();
        // A success clears the failure state outright: keeping the timestamp
        // would leave the indicator DOWN until the window expires even though
        // MIS is demonstrably answering again.
        lastError.set(null);
        lastFailureAt.set(null);
    }

    void recordFailure(String errorClass) {
        failureCount.incrementAndGet();
        lastError.set(errorClass);
        lastFailureAt.set(Instant.now(clock));
    }

    Instant lastFailureAt() {
        return lastFailureAt.get();
    }

    @Override
    public Health health() {
        Instant failedAt = lastFailureAt.get();
        String error = lastError.get();
        boolean recent = failedAt != null
                && Duration.between(failedAt, Instant.now(clock)).compareTo(FAILURE_WINDOW) < 0;
        boolean configured = properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank();
        Health.Builder builder = recent ? Health.down() : Health.up();
        builder.withDetail("misBaseUrl", configured ? properties.getBaseUrl() : "not configured")
                .withDetail("misConfigured", configured)
                .withDetail("lastError", error == null ? "none" : error)
                .withDetail("successes", successCount.get())
                .withDetail("failures", failureCount.get());
        if (recent) {
            builder.withDetail("since", failedAt);
        }
        return builder.build();
    }
}
