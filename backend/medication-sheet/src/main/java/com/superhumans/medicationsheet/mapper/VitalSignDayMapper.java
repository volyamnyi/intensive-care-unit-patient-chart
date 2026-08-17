package com.superhumans.medicationsheet.mapper;

import com.superhumans.medicationsheet.dto.VitalSignDayResponse;
import com.superhumans.medicationsheet.entity.VitalSignDay;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = DateTimeFormatter.class)
public interface VitalSignDayMapper {

    @Mapping(target = "id", expression = "java(day.getId().toString())")
    @Mapping(target = "vitalListId", expression = "java(day.getVitalList().getId().toString())")
    @Mapping(target = "dayDate", expression = "java(day.getDayDate().format(DateTimeFormatter.ISO_LOCAL_DATE))")
    VitalSignDayResponse toResponse(VitalSignDay day);
}
