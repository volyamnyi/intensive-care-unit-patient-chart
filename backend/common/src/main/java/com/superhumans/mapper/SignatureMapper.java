package com.superhumans.mapper;

import com.superhumans.dto.SignResponse;
import com.superhumans.entity.Signature;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SignatureMapper {

    @Mapping(target = "signatureId", source = "id")
    @Mapping(target = "clinicalDayId", source = "clinicalDay.id")
    SignResponse toResponse(Signature entity);
}
