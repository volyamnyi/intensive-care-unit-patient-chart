package com.superhumans.service;

import com.superhumans.icu.entity.ClinicalScale;
import com.superhumans.entity.core.UserRole;
import com.superhumans.exception.NotFoundException;
import com.superhumans.icu.repository.ClinicalScaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Authorization for clinical scales. Instead of a hard-coded role map, access
 * is derived from the dynamic permission matrix: APACHE II/SOFA require
 * {@code SCALE_APACHE_SOFA}, CAM-ICU/Браден/RASS require
 * {@code SCALE_CAMICU_BRADEN_RASS}. Automatic scales (e.g. GCS) are computed
 * by the system and bypass the check. Administrators can change these grants
 * through the admin RBAC interface.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScaleAuthorizationService {

    ClinicalScaleRepository clinicalScaleRepository;
    PermissionService permissionService;

    private static final Set<String> APACHE_SOFA_SCALES = Set.of("APACHE II", "SOFA");
    private static final Set<String> CAMICU_BRADEN_RASS_SCALES = Set.of("CAM-ICU", "Браден", "RASS");

    public void assertCanCreate(UUID scaleId, UserRole role) {
        ClinicalScale scale = clinicalScaleRepository.findById(scaleId)
                .orElseThrow(() -> new NotFoundException("Clinical scale not found: " + scaleId));
        assertCanCreate(scale, role);
    }

    public void assertCanCreate(ClinicalScale scale, UserRole role) {
        if (Boolean.TRUE.equals(scale.getIsAutomatic())) return;
        String permission = permissionFor(scale);
        if (permission == null || !permissionService.hasForRole(role, permission)) {
            throw new SecurityException("Role " + role + " is not allowed to create " + scale.getName());
        }
    }

    public void assertCanUpdate(UUID scaleId, UserRole role) {
        ClinicalScale scale = clinicalScaleRepository.findById(scaleId)
                .orElseThrow(() -> new NotFoundException("Clinical scale not found: " + scaleId));
        assertCanUpdate(scale, role);
    }

    public void assertCanUpdate(ClinicalScale scale, UserRole role) {
        if (Boolean.TRUE.equals(scale.getIsAutomatic())) return;
        String permission = permissionFor(scale);
        if (permission == null || !permissionService.hasForRole(role, permission)) {
            throw new SecurityException("Role " + role + " is not allowed to update " + scale.getName());
        }
    }

    private String permissionFor(ClinicalScale scale) {
        if (APACHE_SOFA_SCALES.contains(scale.getName())) {
            return PermissionCatalog.SCALE_APACHE_SOFA;
        }
        if (CAMICU_BRADEN_RASS_SCALES.contains(scale.getName())) {
            return PermissionCatalog.SCALE_CAMICU_BRADEN_RASS;
        }
        return null;
    }
}
