package com.superhumans.service;

import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditIntegrityException;
import com.superhumans.audit.AuditEventSerializer;
import com.superhumans.audit.AuditOutboxStore;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** At-least-once local-outbox relay. The central event ID is the idempotency/dedupe key. */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditEventRelay {

    static final int MAX_BACKOFF_SECONDS = 900;

    final List<AuditOutboxStore> stores;
    final AuditEventSerializer serializer;
    final AuditEventPersistenceService persistenceService;
    final MeterRegistry meterRegistry;
    final Map<String, AtomicLong> backlog = new ConcurrentHashMap<>();

    @Value("${app.audit.relay.batch-size:20}")
    int batchSize;

    @Value("${app.audit.relay.lease-seconds:60}")
    int leaseSeconds;

    @Value("${app.audit.relay.max-attempts:12}")
    int maxAttempts;

    @PostConstruct
    void registerGauges() {
        stores.forEach(store -> {
            AtomicLong value = backlog.computeIfAbsent(store.module(), ignored -> new AtomicLong());
            Gauge.builder("audit.outbox.backlog", value, AtomicLong::get)
                    .tag("module", store.module())
                    .register(meterRegistry);
        });
    }

    @Scheduled(fixedDelayString = "${app.audit.relay.poll-ms:1000}")
    public void poll() {
        for (AuditOutboxStore store : stores) {
            try {
                List<AuditOutboxStore.ClaimedAuditEvent> batch = store.claimBatch(batchSize, leaseSeconds);
                for (AuditOutboxStore.ClaimedAuditEvent claimed : batch) {
                    deliver(store, claimed);
                }
                backlog.computeIfAbsent(store.module(), ignored -> new AtomicLong())
                        .set(store.pendingCount());
            } catch (RuntimeException exception) {
                increment("audit.outbox.poll.failure", store.module());
                log.error("Audit outbox poll failed module={} errorType={}",
                        store.module(), exception.getClass().getSimpleName());
            }
        }
    }

    private void deliver(AuditOutboxStore store, AuditOutboxStore.ClaimedAuditEvent claimed) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            AuditEvent event = serializer.deserialize(claimed.payload());
            if (!event.auditId().equals(claimed.auditId()) || !event.module().equals(store.module())) {
                store.markDead(claimed.auditId(), "AUDIT_OUTBOX_IDENTITY_MISMATCH");
                increment("audit.outbox.events.dead", store.module());
                increment("audit.integrity.failures", store.module());
                return;
            }
            if (!serializer.sha256(event).equals(claimed.payloadHash())) {
                store.markDead(claimed.auditId(), "AUDIT_OUTBOX_HASH_MISMATCH");
                increment("audit.outbox.events.dead", store.module());
                increment("audit.integrity.failures", store.module());
                return;
            }

            persistenceService.persist(event, claimed.payloadHash());
            store.markDelivered(claimed.auditId());
            increment("audit.outbox.events.delivered", store.module());
        } catch (IllegalArgumentException exception) {
            store.markDead(claimed.auditId(), "AUDIT_OUTBOX_INVALID_PAYLOAD");
            increment("audit.outbox.events.dead", store.module());
            log.error("Audit outbox payload rejected module={} auditId={} errorType={}",
                    store.module(), claimed.auditId(), exception.getClass().getSimpleName());
        } catch (AuditIntegrityException exception) {
            store.markDead(claimed.auditId(), "AUDIT_CANONICAL_HASH_CONFLICT");
            increment("audit.outbox.events.dead", store.module());
            increment("audit.integrity.failures", store.module());
            log.error("Audit integrity conflict module={} auditId={}", store.module(), claimed.auditId());
        } catch (DataAccessException exception) {
            scheduleRetry(store, claimed, "AUDIT_STORE_UNAVAILABLE");
        } catch (RuntimeException exception) {
            scheduleRetry(store, claimed, "AUDIT_RELAY_FAILURE");
        } finally {
            sample.stop(Timer.builder("audit.relay.delivery.duration")
                    .tag("module", store.module())
                    .register(meterRegistry));
        }
    }

    private void scheduleRetry(AuditOutboxStore store,
                               AuditOutboxStore.ClaimedAuditEvent claimed,
                               String errorCode) {
        int exponent = Math.min(Math.max(claimed.attempts() - 1, 0), 9);
        int delaySeconds = Math.min(1 << exponent, MAX_BACKOFF_SECONDS);
        store.scheduleRetry(claimed.auditId(), claimed.attempts(), maxAttempts, delaySeconds, errorCode);
        if (claimed.attempts() >= maxAttempts) {
            increment("audit.outbox.events.dead", store.module());
            log.error("Audit event moved to dead state module={} auditId={} errorCode={}",
                    store.module(), claimed.auditId(), errorCode);
        } else {
            increment("audit.outbox.events.retry", store.module());
            log.warn("Audit event delivery will retry module={} auditId={} attempt={} errorCode={}",
                    store.module(), claimed.auditId(), claimed.attempts(), errorCode);
        }
    }

    private void increment(String name, String module) {
        Counter.builder(name).tag("module", module).register(meterRegistry).increment();
    }
}
