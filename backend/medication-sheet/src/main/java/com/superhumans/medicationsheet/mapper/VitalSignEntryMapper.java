package com.superhumans.medicationsheet.mapper;

import com.superhumans.medicationsheet.dto.VitalSignEntryPatchRequest;
import com.superhumans.medicationsheet.dto.VitalSignEntryRequest;
import com.superhumans.medicationsheet.dto.VitalSignEntryResponse;
import com.superhumans.medicationsheet.entity.VitalSignEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VitalSignEntryMapper {

    @Mapping(target = "id", expression = "java(entry.getId().toString())")
    @Mapping(target = "dayId", expression = "java(entry.getDay().getId().toString())")
    VitalSignEntryResponse toResponse(VitalSignEntry entry);

    VitalSignEntry toEntity(VitalSignEntryRequest request);
    VitalSignEntry toEntity(VitalSignEntryPatchRequest request);
}
