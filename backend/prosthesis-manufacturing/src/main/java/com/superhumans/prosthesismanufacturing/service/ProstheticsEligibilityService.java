package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsCandidateResponse;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prosthetics candidacy use-case (Phase 6, #259): which patients under
 * treatment the prosthetist may start a process for.
 * <p>
 * Rule: {@code departmentId ∈ {19,27,37} AND EXISTS document.templateId ∈ {120,121}}.
 * The service composes the Phase 3 base source
 * ({@code getAllPatientsUnderTreatment}) with the Phase 4 document source
 * ({@code getPatientDocuments}) and the local order registry, and returns
 * ready-to-render candidates — orchestration lives here, never in the
 * controller or the frontend.
 * <p>
 * <b>Assumptions (epic blockers still open):</b> department IDs 19/27/37
 * (blocker (b)), template IDs 120/121 (blocker (c)) and the documents-cache
 * TTL below (blocker (f)) are plan values kept in exactly one place — this
 * class. Only the mapping changes when the MIS owner confirms them.
 * <p>
 * <b>Degradation:</b> a per-patient MIS document error never fails the list —
 * the patient stays with {@code documentsUnknown=true} and empty documents
 * (fail-open with an explicit mark, debug log without PII). A patient with no
 * local orders, a non-matching department, or (with healthy documents) no
 * 120/121 document is excluded silently. A failure of the base patient source
 * itself propagates — there is nothing to assess without it.
 * <p>
 * <b>N+1:</b> MIS exposes no batch document call, so the first pass costs
 * 1 (patients) + N (documents) calls. This is bounded by parallel document
 * fetches over the JVM-managed common pool and a short-lived TTL cache:
 * repeat calls within {@link #DOCUMENTS_CACHE_TTL} reuse cached documents
 * (failed fetches are never cached, so recovery is immediate). Deterministic
 * output order (by MIS patient id) regardless of fetch completion order.
 * <p>
 * Thread-safety: stateless apart from the concurrent documents cache.
 */
@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProstheticsEligibilityService {

    /** Assumption for blocker (b): prosthetics departments. Single place. */
    static final Set<Long> ELIGIBLE_DEPARTMENT_IDS = Set.of(19L, 27L, 37L);
    /** Assumption for blocker (c): order/conclusion templates. Single place. */
    static final Set<Long> ELIGIBLE_DOCUMENT_TEMPLATE_IDS = Set.of(120L, 121L);
    /** Assumption for blocker (f): documents-cache TTL. Single place. */
    static final Duration DOCUMENTS_CACHE_TTL = Duration.ofMinutes(5);

    MisService misService;
    ProstheticsOrderService orderService;
    ProstheticsPatientService patientService;
    ProstheticsPatientRepository patientRepository;
    Clock clock;

    /**
     * Primary constructor used by Spring (system clock).
     */
    @Autowired
    public ProstheticsEligibilityService(MisService misService,
                                         ProstheticsOrderService orderService,
                                         ProstheticsPatientService patientService,
                                         ProstheticsPatientRepository patientRepository) {
        this(misService, orderService, patientService, patientRepository, Clock.systemUTC());
    }

    /**
     * Full constructor (tests inject a fixed {@link Clock} for TTL expiry).
     */
    public ProstheticsEligibilityService(MisService misService,
                                         ProstheticsOrderService orderService,
                                         ProstheticsPatientService patientService,
                                         ProstheticsPatientRepository patientRepository,
                                         Clock clock) {
        this.misService = misService;
        this.orderService = orderService;
        this.patientService = patientService;
        this.patientRepository = patientRepository;
        this.clock = clock;
    }

    record CachedDocuments(List<DocumentMisDTO> documents, Instant loadedAt) {
    }

    Map<Long, CachedDocuments> documentsCache = new ConcurrentHashMap<>();

    /**
     * Returns all eligible candidates, sorted by MIS patient id.
     */
    @Transactional(readOnly = true)
    public List<ProstheticsCandidateResponse> getCandidates() {
        return getCandidates(null);
    }

    /**
     * Returns eligible candidates whose merged patient name or MIS id contains
     * {@code query} (case-insensitive; blank matches all). Powers the setup
     * search box (Phase 9, #262) without downloading the roster twice.
     */
    @Transactional(readOnly = true)
    public List<ProstheticsCandidateResponse> getCandidates(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        return misService.getAllPatientsUnderTreatment().parallelStream()
                .filter(p -> p.getId() != null && p.getDepartmentId() != null
                        && ELIGIBLE_DEPARTMENT_IDS.contains(p.getDepartmentId()))
                .map(this::assess)
                .flatMap(Optional::stream)
                .filter(c -> needle.isEmpty() || matchesQuery(c, needle))
                .sorted(Comparator.comparing(c -> Long.valueOf(c.getPatient().getId())))
                .toList();
    }

    private static boolean matchesQuery(ProstheticsCandidateResponse candidate, String needle) {
        String name = candidate.getPatient().getPib();
        return (name != null && name.toLowerCase(java.util.Locale.ROOT).contains(needle))
                || candidate.getPatient().getId().contains(needle);
    }

    private Optional<ProstheticsCandidateResponse> assess(PatientDTO mis) {
        String key = String.valueOf(mis.getId());
        List<ProstheticsOrderResponse> orders = orderService.list(key, null);
        if (orders.isEmpty()) {
            return Optional.empty();
        }
        List<DocumentMisDTO> documents;
        boolean unknown = false;
        try {
            documents = matchingDocuments(mis.getId(), documentsFor(mis.getId()));
        } catch (Exception e) {
            log.debug("MIS documents unavailable for patient {}, marking documentsUnknown", mis.getId());
            documents = List.of();
            unknown = true;
        }
        if (!unknown && documents.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ProstheticsCandidateResponse.builder()
                .patient(patientService.merge(mis, patientRepository.findById(key)))
                .orders(orders)
                .documents(documents)
                .documentsUnknown(unknown)
                .build());
    }

    private List<DocumentMisDTO> matchingDocuments(Long misId, List<DocumentMisDTO> documents) {
        return documents.stream()
                .filter(d -> d.getDocumentTemplateId() != null
                        && ELIGIBLE_DOCUMENT_TEMPLATE_IDS.contains(d.getDocumentTemplateId()))
                .toList();
    }

    private List<DocumentMisDTO> documentsFor(Long misId) {
        CachedDocuments cached = documentsCache.get(misId);
        if (cached != null && cached.loadedAt().plus(DOCUMENTS_CACHE_TTL).isAfter(clock.instant())) {
            return cached.documents();
        }
        List<DocumentMisDTO> fresh = misService.getPatientDocuments(misId);
        documentsCache.put(misId, new CachedDocuments(fresh == null ? List.of() : fresh, clock.instant()));
        return fresh == null ? List.of() : fresh;
    }

    /**
     * Test hook: drops all cached documents.
     */
    void clearDocumentsCache() {
        documentsCache.clear();
    }
}
