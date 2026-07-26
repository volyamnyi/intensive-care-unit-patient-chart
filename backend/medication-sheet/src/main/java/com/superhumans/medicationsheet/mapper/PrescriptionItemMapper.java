package com.superhumans.medicationsheet.mapper;

import com.superhumans.medicationsheet.dto.PrescriptionItemAddRequest;
import com.superhumans.medicationsheet.dto.PrescriptionItemResponse;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrescriptionItemMapper {

    @Mapping(target = "listId", source = "list.id")
    PrescriptionItemResponse toResponse(PrescriptionItem entity);

    PrescriptionItem toEntity(PrescriptionItemAddRequest request);
}
