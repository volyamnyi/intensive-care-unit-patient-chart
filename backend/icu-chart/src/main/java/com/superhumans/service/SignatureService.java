package com.superhumans.service;

import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.Signature;
import com.superhumans.exception.BusinessException;
import com.superhumans.exception.ErrorCode;
import com.superhumans.icu.repository.SignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SignatureService {

    SignatureRepository signatureRepository;

    @Transactional
    public Signature createSignature(ClinicalDay day, Long userId, String role, String hash,
            String certSerialNumber, String certIssuer, String certSubject,
            LocalDateTime certValidFrom, LocalDateTime certValidUntil) {
        LocalDateTime signedAt = LocalDateTime.now();
        Signature signature = Signature.builder()
                .clinicalDay(day)
                .userId(userId)
                .role(role)
                .signedAt(signedAt)
                .hash(calculateHash(day, userId, role, signedAt))
                .certSerialNumber(certSerialNumber)
                .certIssuer(certIssuer)
                .certSubject(certSubject)
                .certValidFrom(certValidFrom)
                .certValidUntil(certValidUntil)
                .status("ACTIVE")
                .build();
        signature.setCreatedBy(userId);
        signature.setUpdatedBy(userId);
        return signatureRepository.save(signature);
    }

    private String calculateHash(ClinicalDay day, Long userId, String role, LocalDateTime signedAt) {
        String snapshot = String.join("|",
                value(day.getId()),
                value(day.getEpisode() == null ? null : day.getEpisode().getId()),
                value(day.getDayNumber()),
                value(day.getStartDateTime()),
                value(day.getEndDateTime()),
                value(day.getStatus()),
                value(day.getDoctorSigned()),
                value(day.getNurseSigned()),
                value(day.getClosedAt()),
                value(day.getWeightKg()),
                value(userId),
                value(role),
                value(signedAt));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(snapshot.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
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
