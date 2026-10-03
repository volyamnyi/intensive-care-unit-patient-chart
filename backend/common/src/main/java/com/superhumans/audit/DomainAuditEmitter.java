package com.superhumans.audit;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Single collaborator used by domain services to emit canonical audit events.
 * Attaches request correlation and the current operation root, then appends to
 * the module-local durable outbox (feature modules) or the core store
 * (platform module) — always inside the caller's business transaction.
 * Never throws: a recording failure is logged so auditing cannot break
 * the business operation (policy §B4).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainAuditEmitter {

    private final AuditEventFactory eventFactory;
    @Qualifier("platformAuditEventWriter")
    private final AuditEventWriter platformAuditEventWriter;
    private final List<AuditOutboxStore> outboxStores;

    private Map<String, AuditOutboxStore> storesByModule() {
        return outboxStores.stream()
                .collect(Collectors.toUnmodifiableMap(AuditOutboxStore::module, Function.identity()));
    }

    public void emit(String expectedModule, java.util.function.Supplier<AuditEvent> eventSupplier) {
        AuditEvent event = null;
        try {
            event = eventSupplier.get();
            AuditEvent enriched = eventFactory.attachRequestContext(event);
            UUID rootId = AuditOperationContext.currentRootId();
            if (rootId != null && enriched.parentAuditId() == null
                    && !rootId.equals(enriched.auditId())) {
                enriched = enriched.toBuilder().parentAuditId(rootId).build();
            }
            if (!expectedModule.equals(enriched.module())) {
                log.warn("Audit module routing follows the event, not the call site: "
                        + "expected={} actual={} action={}",
                        expectedModule, enriched.module(), enriched.action());
            }
            writerFor(enriched.module()).append(enriched);
        } catch (RuntimeException exception) {
            log.warn("Domain audit emission failed module={} action={} errorType={}",
                    expectedModule, event == null ? null : event.action(),
                    exception.getClass().getSimpleName());
        }
    }

    public AuditOperationContext.Scope beginOperation(UUID rootAuditId) {
        return AuditOperationContext.begin(rootAuditId);
    }

    private AuditEventWriter writerFor(String module) {
        if ("platform".equals(module)) {
            return platformAuditEventWriter;
        }
        AuditOutboxStore store = storesByModule().get(module);
        if (store == null) {
            throw new IllegalArgumentException("Unknown audit module: " + module);
        }
        return store;
    }
}
