package com.superhumans.medicationsheet.mapper;

import com.superhumans.medicationsheet.dto.MedicineCatalogResponse;
import com.superhumans.mis.dto.MedicineMisDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MedicineCatalogMapper {

    @Mapping(target = "isHighRisk", constant = "false")
    MedicineCatalogResponse toResponse(MedicineMisDTO dto);
}
