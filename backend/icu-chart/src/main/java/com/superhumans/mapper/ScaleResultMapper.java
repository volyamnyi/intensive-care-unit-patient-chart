package com.superhumans.mapper;

import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.icu.entity.ScaleResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ScaleResultMapper {

    @Mapping(target = "clinicalDayId", source = "clinicalDay.id")
    @Mapping(target = "episodeId", source = "episodeId")
    @Mapping(target = "scaleId", source = "scale.id")
    @Mapping(target = "scaleName", source = "scale.name")
    ScaleResultResponse toResponse(ScaleResult entity);
}
