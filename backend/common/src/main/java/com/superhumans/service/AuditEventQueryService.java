package com.superhumans.service;

import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventFilter;
import com.superhumans.audit.AuditEventSerializer;
import com.superhumans.audit.LegacyAuditChecksum;
import com.superhumans.dto.AuditChangeResponse;
import com.superhumans.dto.AuditEventDetailResponse;
import com.superhumans.dto.AuditEventSummaryResponse;
import com.superhumans.dto.AuditObjectHistoryResponse;
import com.superhumans.dto.AuditTargetRefResponse;
import com.superhumans.dto.LegacyEventDetailResponse;
import com.superhumans.entity.core.AuditEventEntity;
import com.superhumans.entity.core.AuditEventTargetEntity;
import com.superhumans.entity.core.AuditLegacyEvent;
import com.superhumans.exception.NotFoundException;
import com.superhumans.repository.core.AuditEventRepository;
import com.superhumans.repository.core.AuditEventTargetRepository;
import com.superhumans.repository.core.AuditLegacyEventRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the Audit v2 event store (F7–F8): combined-filter search with
 * stable pagination, event detail with relations and integrity state, and
 * chronological object history with root/child linkage. Backfilled legacy
 * rows (§H.4) are served through the same API, explicitly marked.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditEventQueryService {

    AuditEventRepository eventRepository;
    AuditEventTargetRepository targetRepository;
    AuditLegacyEventRepository legacyEventRepository;
    AuditEventSerializer serializer;
    PermissionService permissionService;

    public Page<AuditEventSummaryResponse> search(AuditEventFilter filter, Pageable pageable) {
        if (filter != null && "legacy".equals(filter.module())) {
            return searchLegacy(filter, pageable);
        }
        Page<AuditEventEntity> page = eventRepository.findAll(
                specification(filter), stablePageable(pageable));
        return page.map(AuditEventQueryService::toSummary);
    }

    /**
     * Legacy-only branch (F8, §H.4): only action/outcome/period/target
     * filters are meaningful on backfilled rows — legacy rows carry no
     * actor login, correlation, request/action IDs, actor type, criticality
     * or functional area, so combining any of those yields an empty page
     * rather than silently ignoring the filter.
     */
    Page<AuditEventSummaryResponse> searchLegacy(AuditEventFilter filter, Pageable pageable) {
        if (filter.actorLogin() != null || filter.correlationId() != null
                || filter.requestId() != null || filter.userActionId() != null
                || filter.actorType() != null || filter.criticality() != null
                || filter.functionalArea() != null) {
            return Page.empty(stablePageable(pageable));
        }
        Page<AuditLegacyEvent> page = legacyEventRepository.findAll(
                legacySpecification(filter), stablePageable(pageable));
        return page.map(AuditEventQueryService::toLegacySummary);
    }

    @Transactional(readOnly = true)
    public AuditEventDetailResponse detail(UUID auditId) {
        Optional<AuditLegacyEvent> legacy = legacyEventRepository.findById(auditId);
        if (legacy.isPresent()) {
            return toLegacyDetail(legacy.get());
        }
        AuditEventEntity entity = eventRepository.findById(auditId)
                .orElseThrow(() -> new NotFoundException("Audit event not found: " + auditId));
        return toDetail(entity, restrictedDetail());
    }

    @Transactional(readOnly = true)
    public AuditObjectHistoryResponse objectHistory(String entityType, String entityId) {
        List<AuditEventTargetEntity> refs =
                targetRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId);
        List<UUID> auditIds = refs.stream().map(AuditEventTargetEntity::getAuditId).distinct().toList();
        List<AuditEventSummaryResponse> events = new ArrayList<>();
        if (!auditIds.isEmpty()) {
            eventRepository.findAllById(auditIds).stream()
                    .sorted(Comparator.comparing(AuditEventEntity::getOccurredAt)
                            .thenComparing(AuditEventEntity::getAuditId))
                    .map(AuditEventQueryService::toSummary)
                    .forEach(events::add);
        }
        events.addAll(legacyHistory(entityType, entityId));
        events.sort(Comparator.comparing(AuditEventSummaryResponse::getOccurredAt)
                .thenComparing(AuditEventSummaryResponse::getAuditId));
        if (events.size() > 1000) {
            events = new ArrayList<>(events.subList(events.size() - 1000, events.size()));
        }
        return AuditObjectHistoryResponse.builder()
                .entityType(entityType)
                .entityId(entityId)
                .eventCount(events.size())
                .events(List.copyOf(events))
                .build();
    }

    private List<AuditEventSummaryResponse> legacyHistory(String entityType, String entityId) {
        UUID legacyId;
        try {
            legacyId = UUID.fromString(entityId);
        } catch (IllegalArgumentException notUuid) {
            return List.of();
        }
        return legacyEventRepository
                .findByLegacyEntityAndLegacyEntityIdOrderByOccurredAtAsc(entityType, legacyId)
                .stream().map(AuditEventQueryService::toLegacySummary).toList();
    }

    private static Instant occurredAtOfSummary(AuditEventSummaryResponse summary) {
        return summary.getOccurredAt();
    }

    private AuditEventDetailResponse toDetail(AuditEventEntity entity, boolean restricted) {
        AuditEvent event = serializer.deserialize(entity.getEventPayload());
        List<AuditChangeResponse> changes = event.changes() == null ? List.of()
                : event.changes().stream().map(change -> AuditChangeResponse.builder()
                        .field(change.field())
                        .type(change.type().name())
                        .dataClass(change.dataClass().name())
                        .oldValue(restricted ? change.oldValue() : null)
                        .newValue(restricted ? change.newValue() : null)
                        .valuesRedacted(!restricted
                                && (change.oldValue() != null || change.newValue() != null))
                        .build()).toList();
        List<AuditTargetRefResponse> targets = targetRepository.findByAuditId(entity.getAuditId())
                .stream().map(ref -> AuditTargetRefResponse.builder()
                        .relationType(ref.getRelationType())
                        .entityType(ref.getEntityType())
                        .entityId(ref.getEntityId())
                        .businessKey(ref.getBusinessKey())
                        .build()).toList();
        List<AuditEventSummaryResponse> children = eventRepository
                .findByParentAuditIdOrderByOccurredAtAsc(entity.getAuditId())
                .stream().map(AuditEventQueryService::toSummary).toList();
        List<Map<String, String>> externalCalls = event.externalCalls() == null ? List.of()
                : event.externalCalls().stream().map(call -> Map.of(
                        "integration", call.integration(),
                        "operation", call.operation(),
                        "callId", call.callId() == null ? "" : call.callId(),
                        "outcome", call.outcome() == null ? "" : call.outcome())).toList();
        boolean integrityVerified = entity.getIntegrityHash() != null
                && entity.getIntegrityHash().equals(serializer.sha256(event));
        List<String> roles = event.actor() == null || event.actor().roles() == null ? List.of()
                : event.actor().roles().stream().sorted().toList();
        return AuditEventDetailResponse.builder()
                .auditId(entity.getAuditId())
                .occurredAt(entity.getOccurredAt())
                .recordedAt(entity.getRecordedAt())
                .eventClass(entity.getEventClass())
                .criticality(entity.getCriticality())
                .module(entity.getModule())
                .functionalArea(entity.getFunctionalArea())
                .action(entity.getAction())
                .actionType(entity.getActionType())
                .actorType(entity.getActorType())
                .actorId(entity.getActorId())
                .actorLogin(entity.getActorLogin())
                .actorDisplayName(entity.getActorDisplayName())
                .actorRoles(roles)
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .businessKey(entity.getBusinessKey())
                .outcome(entity.getOutcome())
                .errorCode(event.errorCode())
                .reasonCode(event.reasonCode())
                .requestId(entity.getRequestId())
                .userActionId(entity.getUserActionId())
                .correlationId(entity.getCorrelationId())
                .parentAuditId(entity.getParentAuditId())
                .source(event.source() == null ? null : event.source().name())
                .httpMethod(event.httpContext() == null ? null : event.httpContext().method())
                .routeTemplate(event.httpContext() == null ? null : event.httpContext().routeTemplate())
                .ipAddress(event.ipAddress())
                .durationMs(event.durationMs())
                .affectedRecords(event.affectedRecords())
                .changes(changes)
                .targets(targets)
                .children(children)
                .externalCalls(externalCalls)
                .metadata(event.metadata() == null ? Map.of() : event.metadata())
                .integrityHash(entity.getIntegrityHash())
                .integrityVerified(integrityVerified)
                .restrictedDetail(restricted)
                .build();
    }

    private boolean restrictedDetail() {
        if (permissionService.has("AUDIT_SECURITY_ACCESS")) {
            return true;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_AUDITOR".equals(authority.getAuthority()));
    }

    private static AuditEventSummaryResponse toLegacySummary(AuditLegacyEvent entity) {
        return AuditEventSummaryResponse.builder()
                .auditId(entity.getAuditId())
                .occurredAt(entity.getOccurredAt())
                .eventClass("LEGACY")
                .module("legacy")
                .functionalArea("legacy")
                .action(entity.getLegacyKind())
                .actionType("LEGACY")
                .criticality("UNKNOWN")
                .actorType(entity.getLegacyUserId() == null ? "UNKNOWN" : "USER")
                .actorLogin(null)
                .targetType(entity.getLegacyEntity())
                .targetId(entity.getLegacyEntityId() == null ? null
                        : entity.getLegacyEntityId().toString())
                .outcome(entity.getOutcome())
                .legacy(true)
                .build();
    }

    private static AuditEventDetailResponse toLegacyDetail(AuditLegacyEvent entity) {
        String recomputed = LegacyAuditChecksum.sha256(LegacyAuditChecksum.canonical(
                entity.getLegacyEntity(),
                entity.getLegacyEntityId(),
                entity.getLegacyAction(),
                entity.getLegacyUserId(),
                entity.getLegacyOldValue(),
                entity.getLegacyNewValue(),
                entity.getLegacyCorrelationId(),
                entity.getLegacyDetails(),
                entity.getLegacyIpAddress(),
                entity.getLegacyUserRole(),
                entity.getLegacyIsDeleted(),
                entity.getOccurredAt() == null ? null
                        : LocalDateTime.ofInstant(entity.getOccurredAt(), ZoneOffset.UTC),
                entity.getSourceId()));
        AuditTargetRefResponse primary = AuditTargetRefResponse.builder()
                .relationType("PRIMARY")
                .entityType(entity.getLegacyEntity())
                .entityId(entity.getLegacyEntityId() == null ? null
                        : entity.getLegacyEntityId().toString())
                .businessKey(null)
                .build();
        return AuditEventDetailResponse.builder()
                .auditId(entity.getAuditId())
                .occurredAt(entity.getOccurredAt())
                .recordedAt(entity.getBackfilledAt())
                .eventClass("LEGACY")
                .criticality("UNKNOWN")
                .module("legacy")
                .functionalArea("legacy")
                .action(entity.getLegacyKind())
                .actionType("LEGACY")
                .actorType(entity.getLegacyUserId() == null ? "UNKNOWN" : "USER")
                .actorId(entity.getLegacyUserId() == null ? null : entity.getLegacyUserId().toString())
                .targetType(entity.getLegacyEntity())
                .targetId(entity.getLegacyEntityId() == null ? null
                        : entity.getLegacyEntityId().toString())
                .outcome(entity.getOutcome())
                .changes(List.of())
                .targets(primary.getEntityId() == null ? List.of() : List.of(primary))
                .children(List.of())
                .externalCalls(List.of())
                .metadata(Map.of())
                .integrityHash(entity.getChecksum())
                .integrityVerified(entity.getChecksum() != null && entity.getChecksum().equals(recomputed))
                .restrictedDetail(false)
                .legacy(true)
                .legacyDetail(LegacyEventDetailResponse.builder()
                        .legacyKind(entity.getLegacyKind())
                        .sourceTable(entity.getSourceTable())
                        .sourceId(entity.getSourceId() == null ? null : entity.getSourceId().toString())
                        .legacyEntity(entity.getLegacyEntity())
                        .legacyEntityId(entity.getLegacyEntityId() == null ? null
                                : entity.getLegacyEntityId().toString())
                        .legacyAction(entity.getLegacyAction())
                        .legacyUserId(entity.getLegacyUserId())
                        .legacyUserRole(entity.getLegacyUserRole())
                        .legacyIpAddress(entity.getLegacyIpAddress())
                        .legacyOldValue(entity.getLegacyOldValue())
                        .legacyNewValue(entity.getLegacyNewValue())
                        .legacyDetails(entity.getLegacyDetails())
                        .legacyCorrelationId(entity.getLegacyCorrelationId())
                        .legacyIsDeleted(entity.getLegacyIsDeleted())
                        .schemaVersion(entity.getSchemaVersion())
                        .contextCompleteness(entity.getContextCompleteness())
                        .timestampPrecision(entity.getTimestampPrecision())
                        .outcome(entity.getOutcome())
                        .checksum(entity.getChecksum())
                        .backfilledAt(entity.getBackfilledAt() == null ? null
                                : entity.getBackfilledAt().toString())
                        .build())
                .build();
    }

    private static AuditEventSummaryResponse toSummary(AuditEventEntity entity) {
        return AuditEventSummaryResponse.builder()
                .auditId(entity.getAuditId())
                .occurredAt(entity.getOccurredAt())
                .eventClass(entity.getEventClass())
                .module(entity.getModule())
                .functionalArea(entity.getFunctionalArea())
                .action(entity.getAction())
                .actionType(entity.getActionType())
                .criticality(entity.getCriticality())
                .actorType(entity.getActorType())
                .actorLogin(entity.getActorLogin())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .outcome(entity.getOutcome())
                .correlationId(entity.getCorrelationId())
                .userActionId(entity.getUserActionId())
                .parentAuditId(entity.getParentAuditId())
                .build();
    }

    private static Pageable stablePageable(Pageable pageable) {
        Sort stable = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("auditId"));
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 20, stable);
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().isSorted() ? pageable.getSort().and(stable) : stable);
    }

    private static Specification<AuditEventEntity> specification(AuditEventFilter filter) {
        return (root, query, criteria) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter == null) {
                return criteria.conjunction();
            }
            if (filter.actorLogin() != null) {
                predicates.add(criteria.equal(root.get("actorLogin"), filter.actorLogin()));
            }
            if (filter.module() != null) {
                predicates.add(criteria.equal(root.get("module"), filter.module()));
            }
            if (filter.functionalArea() != null) {
                predicates.add(criteria.equal(root.get("functionalArea"), filter.functionalArea()));
            }
            if (filter.action() != null) {
                predicates.add(criteria.equal(root.get("action"), filter.action()));
            }
            if (filter.targetType() != null) {
                predicates.add(criteria.equal(root.get("targetType"), filter.targetType()));
            }
            if (filter.targetId() != null) {
                predicates.add(criteria.equal(root.get("targetId"), filter.targetId()));
            }
            if (filter.outcome() != null) {
                predicates.add(criteria.equal(root.get("outcome"), filter.outcome()));
            }
            if (filter.actorType() != null) {
                predicates.add(criteria.equal(root.get("actorType"), filter.actorType()));
            }
            if (filter.criticality() != null) {
                predicates.add(criteria.equal(root.get("criticality"), filter.criticality()));
            }
            if (filter.correlationId() != null) {
                predicates.add(criteria.equal(root.get("correlationId"), filter.correlationId()));
            }
            if (filter.requestId() != null) {
                predicates.add(criteria.equal(root.get("requestId"), filter.requestId()));
            }
            if (filter.userActionId() != null) {
                predicates.add(criteria.equal(root.get("userActionId"), filter.userActionId()));
            }
            if (filter.occurredFrom() != null) {
                predicates.add(criteria.greaterThanOrEqualTo(root.get("occurredAt"), filter.occurredFrom()));
            }
            if (filter.occurredTo() != null) {
                predicates.add(criteria.lessThanOrEqualTo(root.get("occurredAt"), filter.occurredTo()));
            }
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Specification<AuditLegacyEvent> legacySpecification(AuditEventFilter filter) {
        return (root, query, criteria) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter == null) {
                return criteria.conjunction();
            }
            if (filter.action() != null) {
                predicates.add(criteria.equal(root.get("legacyKind"), filter.action()));
            }
            if (filter.targetType() != null) {
                predicates.add(criteria.equal(root.get("legacyEntity"), filter.targetType()));
            }
            if (filter.targetId() != null) {
                try {
                    predicates.add(criteria.equal(root.get("legacyEntityId"),
                            UUID.fromString(filter.targetId())));
                } catch (IllegalArgumentException notUuid) {
                    return criteria.disjunction();
                }
            }
            if (filter.outcome() != null) {
                predicates.add(criteria.equal(root.get("outcome"), filter.outcome()));
            }
            if (filter.occurredFrom() != null) {
                predicates.add(criteria.greaterThanOrEqualTo(root.get("occurredAt"), filter.occurredFrom()));
            }
            if (filter.occurredTo() != null) {
                predicates.add(criteria.lessThanOrEqualTo(root.get("occurredAt"), filter.occurredTo()));
            }
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
    }
}
