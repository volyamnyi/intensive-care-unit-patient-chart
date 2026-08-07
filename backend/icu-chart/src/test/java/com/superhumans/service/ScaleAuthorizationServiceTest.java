package com.superhumans.service;

import com.superhumans.entity.ClinicalScale;
import com.superhumans.entity.UserRole;
import com.superhumans.exception.NotFoundException;
import com.superhumans.repository.ClinicalScaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScaleAuthorizationServiceTest {

    @Mock
    private ClinicalScaleRepository clinicalScaleRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private ScaleAuthorizationService scaleAuthorizationService;

    private UUID scaleId;
    private ClinicalScale apacheScale;
    private ClinicalScale sofaScale;
    private ClinicalScale rassScale;
    private ClinicalScale camIcuScale;
    private ClinicalScale bradenScale;
    private ClinicalScale automaticScale;
    private ClinicalScale unknownScale;

    @BeforeEach
    void setUp() {
        scaleId = UUID.randomUUID();
        apacheScale = createScale("APACHE II", false);
        sofaScale = createScale("SOFA", false);
        rassScale = createScale("RASS", false);
        camIcuScale = createScale("CAM-ICU", false);
        bradenScale = createScale("Браден", false);
        automaticScale = createScale("GCS", true);
        unknownScale = createScale("CustomScale", false);

        // Default matrix: DOCTOR/HOD hold both scale permissions, NURSE only
        // the CAM-ICU/Браден/RASS group.
        lenient().when(permissionService.hasForRole(eq(UserRole.DOCTOR), anyString()))
                .thenAnswer(inv -> isScalePermission(inv.getArgument(1)));
        lenient().when(permissionService.hasForRole(eq(UserRole.HEAD_OF_DEPARTMENT), anyString()))
                .thenAnswer(inv -> isScalePermission(inv.getArgument(1)));
        lenient().when(permissionService.hasForRole(eq(UserRole.NURSE), anyString()))
                .thenAnswer(inv -> PermissionCatalog.SCALE_CAMICU_BRADEN_RASS.equals(inv.getArgument(1)));
    }

    private boolean isScalePermission(String code) {
        return PermissionCatalog.SCALE_APACHE_SOFA.equals(code)
                || PermissionCatalog.SCALE_CAMICU_BRADEN_RASS.equals(code);
    }

    private ClinicalScale createScale(String name, boolean isAutomatic) {
        ClinicalScale scale = ClinicalScale.builder()
                .name(name)
                .isAutomatic(isAutomatic)
                .status("ACTIVE")
                .build();
        scale.setId(scaleId);
        return scale;
    }

    @Test
    void assertCanCreate_doctorAbleToCreateApacheIi() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanCreate(apacheScale, UserRole.DOCTOR));
    }

    @Test
    void assertCanCreate_nurseBlockedFromApacheIi() {
        assertThatThrownBy(() -> scaleAuthorizationService.assertCanCreate(apacheScale, UserRole.NURSE))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("NURSE is not allowed to create APACHE II");
    }

    @Test
    void assertCanCreate_doctorAbleToCreateSofa() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanCreate(sofaScale, UserRole.DOCTOR));
    }

    @Test
    void assertCanCreate_nurseBlockedFromSofa() {
        assertThatThrownBy(() -> scaleAuthorizationService.assertCanCreate(sofaScale, UserRole.NURSE))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("NURSE is not allowed to create SOFA");
    }

    @Test
    void assertCanCreate_nurseAbleToCreateRass() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanCreate(rassScale, UserRole.NURSE));
    }

    @Test
    void assertCanCreate_doctorAbleToCreateRass() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanCreate(rassScale, UserRole.DOCTOR));
    }

    @Test
    void assertCanCreate_nurseAbleToCreateCamIcu() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanCreate(camIcuScale, UserRole.NURSE));
    }

    @Test
    void assertCanCreate_nurseAbleToCreateBraden() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanCreate(bradenScale, UserRole.NURSE));
    }

    @Test
    void assertCanCreate_doctorAbleToCreateBradenPerMatrix() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanCreate(bradenScale, UserRole.DOCTOR));
    }

    @Test
    void assertCanCreate_hodAbleToCreateApacheIiPerMatrix() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanCreate(apacheScale, UserRole.HEAD_OF_DEPARTMENT));
    }

    @Test
    void assertCanCreate_automaticScaleBypassesCheck() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanCreate(automaticScale, UserRole.NURSE));
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanCreate(automaticScale, UserRole.DOCTOR));
    }

    @Test
    void assertCanCreate_unknownScale_allRolesBlocked() {
        assertThatThrownBy(() -> scaleAuthorizationService.assertCanCreate(unknownScale, UserRole.DOCTOR))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("CustomScale");
        assertThatThrownBy(() -> scaleAuthorizationService.assertCanCreate(unknownScale, UserRole.NURSE))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void assertCanCreate_byId_scaleNotFound_throws() {
        when(clinicalScaleRepository.findById(scaleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scaleAuthorizationService.assertCanCreate(scaleId, UserRole.DOCTOR))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void assertCanCreate_byId_doctorAllowed() {
        when(clinicalScaleRepository.findById(scaleId)).thenReturn(Optional.of(apacheScale));

        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanCreate(scaleId, UserRole.DOCTOR));
    }

    @Test
    void assertCanUpdate_doctorAbleToUpdateApacheIi() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanUpdate(apacheScale, UserRole.DOCTOR));
    }

    @Test
    void assertCanUpdate_nurseBlockedFromApacheIi() {
        assertThatThrownBy(() -> scaleAuthorizationService.assertCanUpdate(apacheScale, UserRole.NURSE))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("NURSE is not allowed to update APACHE II");
    }

    @Test
    void assertCanUpdate_nurseAbleToUpdateBraden() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanUpdate(bradenScale, UserRole.NURSE));
    }

    @Test
    void assertCanUpdate_unknownScale_allRolesBlocked() {
        assertThatThrownBy(() -> scaleAuthorizationService.assertCanUpdate(unknownScale, UserRole.DOCTOR))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void assertCanUpdate_automaticScaleBypassesCheck() {
        assertThatNoException()
                .isThrownBy(() -> scaleAuthorizationService.assertCanUpdate(automaticScale, UserRole.NURSE));
    }
}
