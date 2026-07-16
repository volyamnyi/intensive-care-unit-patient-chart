package com.superhumans.service;

import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.Signature;
import com.superhumans.exception.BusinessException;
import com.superhumans.exception.ErrorCode;
import com.superhumans.repository.SignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SignatureService {

    SignatureRepository signatureRepository;

    @Transactional
    public Signature createSignature(ClinicalDay day, UUID userId, String role, String hash) {
        Signature signature = Signature.builder()
                .clinicalDay(day)
                .userId(userId)
                .role(role)
                .signedAt(LocalDateTime.now())
                .hash(hash)
                .status("ACTIVE")
                .build();
        signature.setCreatedBy(userId);
        signature.setUpdatedBy(userId);
        return signatureRepository.save(signature);
    }

    @Transactional
    public void revokeSignaturesByClinicalDay(UUID clinicalDayId) {
        List<Signature> signatures = signatureRepository.findByClinicalDayId(clinicalDayId);
        for (Signature s : signatures) {
            s.setStatus("REVOKED");
            signatureRepository.save(s);
        }
    }

    public boolean hasNurseSignature(UUID clinicalDayId) {
        return signatureRepository.findByClinicalDayIdAndRole(clinicalDayId, "NURSE")
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .isPresent();
    }

    public boolean hasDoctorSignature(UUID clinicalDayId) {
        return signatureRepository.findByClinicalDayIdAndRole(clinicalDayId, "DOCTOR")
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .isPresent();
    }

    public void assertNoNurseSignature(UUID clinicalDayId) {
        if (hasNurseSignature(clinicalDayId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_LOCKED,
                    "Clinical day already has a nurse signature");
        }
    }

    public void assertNoDoctorSignature(UUID clinicalDayId) {
        if (hasDoctorSignature(clinicalDayId)) {
            throw new BusinessException(ErrorCode.DOCUMENT_LOCKED,
                    "Clinical day already has a doctor signature");
        }
    }
}
