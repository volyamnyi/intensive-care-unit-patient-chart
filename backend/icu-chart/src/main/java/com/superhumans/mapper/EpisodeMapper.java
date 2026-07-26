package com.superhumans.mapper;

import com.superhumans.dto.EpisodeCreateRequest;
import com.superhumans.dto.EpisodeResponse;
import com.superhumans.entity.Episode;
import com.superhumans.entity.EpisodeStatus;
import com.superhumans.entity.EpisodeStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = EpisodeStatus.class)
public interface EpisodeMapper {

    EpisodeResponse toResponse(Episode entity);

    @Mapping(target = "patientName", source = "patientName")
    EpisodeResponse toResponse(Episode entity, String patientName);

    @Mapping(target = "status", expression = "java(EpisodeStatus.DRAFT)")
    Episode toEntity(EpisodeCreateRequest request);
}
