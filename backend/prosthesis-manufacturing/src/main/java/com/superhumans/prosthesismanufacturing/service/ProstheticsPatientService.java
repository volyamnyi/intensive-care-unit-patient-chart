package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsPatientResponse;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsPatientMapper;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProstheticsPatientService {

    ProstheticsPatientRepository patientRepository;
    ProstheticsPatientMapper patientMapper;

    @Transactional(readOnly = true)
    public List<ProstheticsPatientResponse> search(String query) {
        List<ProstheticsPatientResponse> patients;
        if (StringUtils.hasText(query)) {
            patients = patientRepository.findByPibContainingIgnoreCase(query.trim())
                    .stream().map(patientMapper::toResponse).toList();
        } else {
            patients = patientRepository.findAll().stream().map(patientMapper::toResponse).toList();
        }
        return patients;
    }

    @Transactional(readOnly = true)
    public ProstheticsPatientResponse get(String id) {
        return patientRepository.findById(id)
                .map(patientMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + id));
    }
}
