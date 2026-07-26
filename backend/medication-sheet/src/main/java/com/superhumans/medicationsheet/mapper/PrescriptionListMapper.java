package com.superhumans.medicationsheet.mapper;

import com.superhumans.medicationsheet.dto.PrescriptionListCreateRequest;
import com.superhumans.medicationsheet.dto.PrescriptionListResponse;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrescriptionListMapper {

    PrescriptionListResponse toResponse(PrescriptionList entity);

    PrescriptionList toEntity(PrescriptionListCreateRequest request);
}
