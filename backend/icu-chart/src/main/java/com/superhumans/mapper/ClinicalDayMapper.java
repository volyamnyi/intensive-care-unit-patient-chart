package com.superhumans.mapper;

import com.superhumans.dto.ClinicalDayCreateRequest;
import com.superhumans.dto.ClinicalDayResponse;
import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.ClinicalDayStatus;
import org.mapstruct.*;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = ClinicalDayStatus.class)
public interface ClinicalDayMapper {

    @Mapping(target = "episodeId", source = "episode.id")
    @Mapping(target = "weightKg", source = "weightKg")
    ClinicalDayResponse toResponse(ClinicalDay entity);

    @AfterMapping
    default void computeBmi(@MappingTarget ClinicalDayResponse response, ClinicalDay day) {
        if (day.getWeightKg() != null && day.getEpisode() != null && day.getEpisode().getHeightCm() != null
                && day.getEpisode().getHeightCm() > 0) {
            double heightM = day.getEpisode().getHeightCm() / 100.0;
            response.setBmi(Math.round(day.getWeightKg() / (heightM * heightM) * 10.0) / 10.0);
        }
    }

    @Mapping(target = "dayNumber", constant = "1")
    @Mapping(target = "status", expression = "java(ClinicalDayStatus.OPEN)")
    @Mapping(target = "doctorSigned", constant = "false")
    @Mapping(target = "nurseSigned", constant = "false")
    ClinicalDay toEntity(ClinicalDayCreateRequest request);
}
