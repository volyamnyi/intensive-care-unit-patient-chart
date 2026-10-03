package com.superhumans.service;

import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventFilter;
import com.superhumans.audit.AuditEventSerializer;
import com.superhumans.dto.AuditChangeResponse;
import com.superhumans.dto.AuditEventDetailResponse;
import com.superhumans.dto.AuditEventSummaryResponse;
import com.superhumans.dto.AuditObjectHistoryResponse;
import com.superhumans.dto.AuditTargetRefResponse;
import com.superhumans.entity.core.AuditEventEntity;
import com.superhumans.entity.core.AuditEventTargetEntity;
import com.superhumans.exception.NotFoundException;
import com.superhumans.repository.core.AuditEventRepository;
import com.superhumans.repository.core.AuditEventTargetRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
 * Read side of the Audit v2 event store (F7): combined-filter search with
 * stable pagination, event detail with relations and integrity state, and
 * chronological object history with root/child linkage.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditEventQueryService {

    AuditEventRepository eventRepository;
    AuditEventTargetRepository targetRepository;
    AuditEventSerializer serializer;
    PermissionService permissionService;

    public Page<AuditEventSummaryResponse> search(AuditEventFilter filter, Pageable pageable) {
        Page<AuditEventEntity> page = eventRepository.findAll(
                specification(filter), stablePageable(pageable));
        return page.map(AuditEventQueryService::toSummary);
    }

    @Transactional(readOnly = true)
    public AuditEventDetailResponse detail(UUID auditId) {
        AuditEventEntity entity = eventRepository.findById(auditId)
                .orElseThrow(() -> new NotFoundException("Audit event not found: " + auditId));
        return toDetail(entity, restrictedDetail());
    }

    @Transactional(readOnly = true)
    public AuditObjectHistoryResponse objectHistory(String entityType, String entityId) {
        List<AuditEventTargetEntity> refs =
                targetRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId);
        List<UUID> auditIds = refs.stream().map(AuditEventTargetEntity::getAuditId).distinct().toList();
        List<AuditEventSummaryResponse> events = auditIds.isEmpty() ? List.of()
                : eventRepository.findAllById(auditIds).stream()
                        .sorted(Comparator.comparing(AuditEventEntity::getOccurredAt)
                                .thenComparing(AuditEventEntity::getAuditId))
                        .map(AuditEventQueryService::toSummary)
                        .toList();
        return AuditObjectHistoryResponse.builder()
                .entityType(entityType)
                .entityId(entityId)
                .eventCount(events.size())
                .events(events)
                .build();
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
}
