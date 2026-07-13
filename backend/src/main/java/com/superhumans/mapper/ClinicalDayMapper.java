package com.superhumans.mapper;

import com.superhumans.dto.ClinicalDayCreateRequest;
import com.superhumans.dto.ClinicalDayResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;

public class ClinicalDayMapper {

    public static ClinicalDayResponse toResponse(ClinicalDay entity) {
        return ClinicalDayResponse.builder()
                .id(entity.getId())
                .episodeId(entity.getEpisode().getId())
                .dayNumber(entity.getDayNumber())
                .startDateTime(entity.getStartDateTime())
                .endDateTime(entity.getEndDateTime())
                .status(entity.getStatus())
                .doctorSigned(entity.getDoctorSigned())
                .nurseSigned(entity.getNurseSigned())
                .closedAt(entity.getClosedAt())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    public static ClinicalDay toEntity(ClinicalDayCreateRequest request) {
        return ClinicalDay.builder()
                .dayNumber(1)
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .status(ClinicalDayStatus.OPEN)
                .doctorSigned(false)
                .nurseSigned(false)
                .build();
    }
}
