package com.superhumans.service;

import com.superhumans.entity.ClinicalScale;
import com.superhumans.entity.UserRole;
import com.superhumans.exception.NotFoundException;
import com.superhumans.repository.ClinicalScaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScaleAuthorizationService {

    ClinicalScaleRepository clinicalScaleRepository;

    static final Map<String, Set<UserRole>> CREATE_PERMISSIONS = Map.of(
            "APACHE II", EnumSet.of(UserRole.DOCTOR),
            "SOFA", EnumSet.of(UserRole.DOCTOR),
            "RASS", EnumSet.of(UserRole.DOCTOR, UserRole.NURSE),
            "CAM-ICU", EnumSet.of(UserRole.DOCTOR, UserRole.NURSE),
            "Браден", EnumSet.of(UserRole.NURSE)
    );

    static final Map<String, Set<UserRole>> UPDATE_PERMISSIONS = Map.of(
            "APACHE II", EnumSet.of(UserRole.DOCTOR),
            "SOFA", EnumSet.of(UserRole.DOCTOR),
            "RASS", EnumSet.of(UserRole.DOCTOR, UserRole.NURSE),
            "CAM-ICU", EnumSet.of(UserRole.DOCTOR, UserRole.NURSE),
            "Браден", EnumSet.of(UserRole.NURSE)
    );

    public void assertCanCreate(UUID scaleId, UserRole role) {
        ClinicalScale scale = clinicalScaleRepository.findById(scaleId)
                .orElseThrow(() -> new NotFoundException("Clinical scale not found: " + scaleId));
        assertCanCreate(scale, role);
    }

    public void assertCanCreate(ClinicalScale scale, UserRole role) {
        if (Boolean.TRUE.equals(scale.getIsAutomatic())) return;
        Set<UserRole> allowed = CREATE_PERMISSIONS.get(scale.getName());
        if (allowed == null || !allowed.contains(role)) {
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
        Set<UserRole> allowed = UPDATE_PERMISSIONS.get(scale.getName());
        if (allowed == null || !allowed.contains(role)) {
            throw new SecurityException("Role " + role + " is not allowed to update " + scale.getName());
        }
    }
}
