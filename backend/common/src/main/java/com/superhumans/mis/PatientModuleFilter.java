package com.superhumans.mis;

import com.superhumans.exception.BadRequestException;
import com.superhumans.mis.dto.PatientDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Module roster filter over MIS patients (Phase 7, #260).
 * <p>
 * Each consuming module sees only its departments: the medication sheet
 * ({@code medication} → surgery 19 + rehabilitation 37), applied on top of
 * the Phase 3 base source. Patients with an unknown/null department never
 * match (consistent with the prosthetics eligibility rule, #259).
 * <p>
 * <b>Assumption (epic blocker (b)):</b> department IDs are plan values kept
 * in exactly one place — {@link #MODULE_DEPARTMENTS}. Only the mapping
 * changes when the MIS owner confirms the real IDs.
 */
public final class PatientModuleFilter {

    /** Medication sheet roster: surgery + rehabilitation (blocker (b) assumption). */
    public static final Set<Long> MEDICATION_DEPARTMENT_IDS = Set.of(19L, 37L);

    /** Intensive-care roster: surgery only (blocker (b) assumption, #261). */
    public static final Set<Long> ICU_DEPARTMENT_IDS = Set.of(19L);

    /** Module name → eligible department IDs. Single place for the mapping. */
    public static final Map<String, Set<Long>> MODULE_DEPARTMENTS =
            Map.of("medication", MEDICATION_DEPARTMENT_IDS, "icu", ICU_DEPARTMENT_IDS);

    private PatientModuleFilter() {
    }

    /**
     * Returns the base list narrowed to the module roster.
     *
     * @throws BadRequestException for an unknown module name (fail fast on
     *                             caller typos instead of silently returning
     *                             the unfiltered roster)
     */
    public static List<PatientDTO> filter(String module, List<PatientDTO> base) {
        Set<Long> eligible = MODULE_DEPARTMENTS.get(module);
        if (eligible == null) {
            throw new BadRequestException("Unknown patient module: " + module);
        }
        if (base == null || base.isEmpty()) {
            return List.of();
        }
        return base.stream()
                .filter(p -> p.getDepartmentId() != null && eligible.contains(p.getDepartmentId()))
                .toList();
    }
}
