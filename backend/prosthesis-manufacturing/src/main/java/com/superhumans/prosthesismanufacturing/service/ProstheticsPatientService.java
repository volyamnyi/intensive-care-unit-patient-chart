package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsPatientResponse;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsPatientMapper;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Patient registry facade.
 * <p>
 * <b>SINGLE SOURCE OF TRUTH POLICY:</b> all demographic patient data (ПІБ, date
 * of birth, sex, address, phone, email, height, weight) comes from the MIS
 * Integration Layer (common module, wiremock at present) — never from the local
 * database. The local {@code prosthetics_patients} table stores ONLY
 * prosthesis-specific clinical data (amputation, stump, clinical state) that the
 * MIS API does not expose.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProstheticsPatientService {

    MisService misService;
    ProstheticsPatientRepository patientRepository;
    ProstheticsPatientMapper patientMapper;

    @Transactional(readOnly = true)
    public List<ProstheticsPatientResponse> search(String query) {
        List<PatientDTO> misPatients;
        try {
            misPatients = misService.searchPatients(query);
        } catch (Exception e) {
            log.warn("MIS patient search failed, falling back to local registry: {}", e.getMessage());
            misPatients = List.of();
        }
        if (misPatients.isEmpty()) {
            return localSearchFallback(query);
        }
        List<ProstheticsPatientResponse> result = new ArrayList<>();
        for (PatientDTO mis : misPatients) {
            result.add(merge(mis, localPatient(mis.getId())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ProstheticsPatientResponse get(String id) {
        Long misId = parseId(id);
        if (misId != null) {
            try {
                Optional<PatientDTO> mis = misService.getPatient(misId);
                if (mis.isPresent()) {
                    return merge(mis.get(), localPatient(misId));
                }
            } catch (Exception e) {
                log.warn("MIS patient lookup failed for id={}, falling back to local registry: {}", id, e.getMessage());
            }
        }
        ProstheticsPatient local = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + id));
        return patientMapper.toResponse(local);
    }

    // ==================== helpers ====================

    /**
     * MIS is the single source of truth for demographics; the local record only
     * contributes prosthesis-specific clinical fields (cause, amputation, stump,
     * clinical state) that MIS does not provide.
     */
    private ProstheticsPatientResponse merge(PatientDTO mis, Optional<ProstheticsPatient> local) {
        ProstheticsPatientResponse.ProstheticsPatientResponseBuilder b = ProstheticsPatientResponse.builder()
                .id(mis.getId() == null ? null : String.valueOf(mis.getId()))
                .pib(mis.getFullName())
                .birthDate(mis.getBirthDate())
                .gender(misSexToLabel(mis.getSexCode()))
                .heightCm(mis.getHeight())
                .weightKg(mis.getWeight())
                .residence(mis.getAddress())
                .phone(mis.getPhone())
                .email(mis.getEmail())
                .socialStatus(null);
        local.ifPresent(l -> {
            b.cause(l.getCause())
                    .amputationDate(l.getAmputationDate())
                    .affectedLimb(l.getAffectedLimb())
                    .amputationLevel(l.getAmputationLevel())
                    .amputationSite(l.getAmputationSite())
                    .healthStatus(l.getHealthStatus())
                    .clinicalState(l.getClinicalState())
                    .stump(l.getStump())
                    .socialStatus(l.getSocialStatus());
        });
        return b.build();
    }

    private Optional<ProstheticsPatient> localPatient(Long misId) {
        if (misId == null) {
            return Optional.empty();
        }
        return patientRepository.findById(String.valueOf(misId));
    }

    private List<ProstheticsPatientResponse> localSearchFallback(String query) {
        List<ProstheticsPatient> patients;
        if (StringUtils.hasText(query)) {
            patients = patientRepository.findByPibContainingIgnoreCase(query.trim());
        } else {
            patients = patientRepository.findAll();
        }
        return patients.stream().map(patientMapper::toResponse).toList();
    }

    private Long parseId(String id) {
        if (id == null || id.isBlank() || !id.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String misSexToLabel(String sexCode) {
        if (sexCode == null || sexCode.isBlank()) {
            return null;
        }
        return switch (sexCode.toUpperCase(java.util.Locale.ROOT)) {
            case "MAL" -> "Чоловіча";
            case "FEM" -> "Жіноча";
            default -> sexCode;
        };
    }
}
