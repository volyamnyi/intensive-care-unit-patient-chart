package com.superhumans.service;

import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.Signature;
import com.superhumans.exception.BusinessException;
import com.superhumans.icu.repository.SignatureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignatureServiceTest {

    @Mock
    private SignatureRepository signatureRepository;

    @InjectMocks
    private SignatureService signatureService;

    @Captor
    private ArgumentCaptor<Signature> sigCaptor;

    private UUID clinicalDayId;
    private Long userId;
    private ClinicalDay clinicalDay;

    @BeforeEach
    void setUp() {
        clinicalDayId = UUID.randomUUID();
        userId = 11L;
        clinicalDay = new ClinicalDay();
        clinicalDay.setId(clinicalDayId);
    }

    @Test
    void createSignature_createsSuccessfully() {
        Signature sig = new Signature();
        sig.setId(UUID.randomUUID());
        sig.setClinicalDay(clinicalDay);
        sig.setUserId(userId);
        sig.setRole("DOCTOR");
        sig.setHash("hash123");
        sig.setStatus("ACTIVE");

        when(signatureRepository.save(any(Signature.class))).thenReturn(sig);

        Signature result = signatureService.createSignature(clinicalDay, userId, "DOCTOR", "hash123",
                null, null, null, null, null);

        verify(signatureRepository).save(sigCaptor.capture());
        assertThat(sigCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(sigCaptor.getValue().getRole()).isEqualTo("DOCTOR");
        assertThat(sigCaptor.getValue().getHash()).isEqualTo("hash123");
        assertThat(sigCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void revokeSignatureByDay_updatesStatusToRevoked() {
        Signature activeSig = new Signature();
        activeSig.setId(UUID.randomUUID());
        activeSig.setClinicalDay(clinicalDay);
        activeSig.setUserId(userId);
        activeSig.setStatus("ACTIVE");

        when(signatureRepository.findByClinicalDayId(clinicalDayId))
                .thenReturn(List.of(activeSig));

        signatureService.revokeSignaturesByClinicalDay(clinicalDayId);

        verify(signatureRepository).save(sigCaptor.capture());
        assertThat(sigCaptor.getValue().getStatus()).isEqualTo("REVOKED");
    }

    @Test
    void hasNurseSignature_returnsTrue() {
        Signature sig = new Signature();
        sig.setStatus("ACTIVE");
        when(signatureRepository.findByClinicalDayIdAndRole(clinicalDayId, "NURSE"))
                .thenReturn(Optional.of(sig));

        boolean result = signatureService.hasNurseSignature(clinicalDayId);

        assertThat(result).isTrue();
    }

    @Test
    void hasNurseSignature_returnsFalse_whenNotExists() {
        when(signatureRepository.findByClinicalDayIdAndRole(clinicalDayId, "NURSE"))
                .thenReturn(Optional.empty());

        boolean result = signatureService.hasNurseSignature(clinicalDayId);

        assertThat(result).isFalse();
    }

    @Test
    void hasNurseSignature_returnsFalse_whenRevoked() {
        Signature sig = new Signature();
        sig.setStatus("REVOKED");
        when(signatureRepository.findByClinicalDayIdAndRole(clinicalDayId, "NURSE"))
                .thenReturn(Optional.of(sig));

        boolean result = signatureService.hasNurseSignature(clinicalDayId);

        assertThat(result).isFalse();
    }

    @Test
    void hasDoctorSignature_returnsTrue() {
        Signature sig = new Signature();
        sig.setStatus("ACTIVE");
        when(signatureRepository.findByClinicalDayIdAndRole(clinicalDayId, "DOCTOR"))
                .thenReturn(Optional.of(sig));

        boolean result = signatureService.hasDoctorSignature(clinicalDayId);

        assertThat(result).isTrue();
    }

    @Test
    void assertNoNurseSignature_throwsWhenExists() {
        Signature sig = new Signature();
        sig.setStatus("ACTIVE");
        when(signatureRepository.findByClinicalDayIdAndRole(clinicalDayId, "NURSE"))
                .thenReturn(Optional.of(sig));

        assertThatThrownBy(() -> signatureService.assertNoNurseSignature(clinicalDayId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void assertNoNurseSignature_passesWhenNotExists() {
        when(signatureRepository.findByClinicalDayIdAndRole(clinicalDayId, "NURSE"))
                .thenReturn(Optional.empty());

        signatureService.assertNoNurseSignature(clinicalDayId);
    }

    @Test
    void assertNoDoctorSignature_throwsWhenExists() {
        Signature sig = new Signature();
        sig.setStatus("ACTIVE");
        when(signatureRepository.findByClinicalDayIdAndRole(clinicalDayId, "DOCTOR"))
                .thenReturn(Optional.of(sig));

        assertThatThrownBy(() -> signatureService.assertNoDoctorSignature(clinicalDayId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void assertNoDoctorSignature_passesWhenNotExists() {
        when(signatureRepository.findByClinicalDayIdAndRole(clinicalDayId, "DOCTOR"))
                .thenReturn(Optional.empty());

        signatureService.assertNoDoctorSignature(clinicalDayId);
    }
}
