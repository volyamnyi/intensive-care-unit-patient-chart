package com.superhumans.mapper;

import com.superhumans.dto.ClinicalDayCreateRequest;
import com.superhumans.dto.ClinicalDayResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.ClinicalDayStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = ClinicalDayStatus.class)
public interface ClinicalDayMapper {

    @Mapping(target = "episodeId", source = "episode.id")
    ClinicalDayResponse toResponse(ClinicalDay entity);

    @Mapping(target = "dayNumber", constant = "1")
    @Mapping(target = "status", expression = "java(ClinicalDayStatus.OPEN)")
    @Mapping(target = "doctorSigned", constant = "false")
    @Mapping(target = "nurseSigned", constant = "false")
    ClinicalDay toEntity(ClinicalDayCreateRequest request);
}
