package com.superhumans.mapper;

import com.superhumans.dto.EpisodeCreateRequest;
import com.superhumans.dto.EpisodeResponse;
import com.superhumans.entity.Episode;
import com.superhumans.entity.EpisodeStatus;

public class EpisodeMapper {

    public static EpisodeResponse toResponse(Episode entity) {
        return toResponse(entity, null);
    }

    public static EpisodeResponse toResponse(Episode entity, String patientName) {
        return EpisodeResponse.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .patientName(patientName)
                .hospitalizationId(entity.getHospitalizationId())
                .departmentId(entity.getDepartmentId())
                .admissionDate(entity.getAdmissionDate())
                .dischargeDate(entity.getDischargeDate())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    public static Episode toEntity(EpisodeCreateRequest request) {
        return Episode.builder()
                .patientId(request.getPatientId())
                .hospitalizationId(request.getHospitalizationId())
                .departmentId(request.getDepartmentId())
                .admissionDate(request.getAdmissionDate())
                .status(EpisodeStatus.DRAFT)
                .build();
    }
}
