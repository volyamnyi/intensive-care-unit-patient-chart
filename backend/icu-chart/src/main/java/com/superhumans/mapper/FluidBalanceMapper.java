package com.superhumans.mapper;

import com.superhumans.dto.FluidBalanceResponse;
import com.superhumans.icu.entity.FluidBalance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FluidBalanceMapper {

    @Mapping(target = "clinicalDayId", source = "clinicalDay.id")
    FluidBalanceResponse toResponse(FluidBalance entity);
}
