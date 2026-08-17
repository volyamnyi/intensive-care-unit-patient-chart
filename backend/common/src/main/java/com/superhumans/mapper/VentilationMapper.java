package com.superhumans.mapper;

import com.superhumans.dto.VentilationResponse;
import com.superhumans.icu.entity.VentilationSettings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VentilationMapper {

    @Mapping(target = "clinicalDayId", source = "clinicalDay.id")
    VentilationResponse toResponse(VentilationSettings entity);
}
