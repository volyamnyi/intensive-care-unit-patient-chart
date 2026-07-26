package com.superhumans.mapper;

import com.superhumans.dto.MedicalNoteResponse;
import com.superhumans.entity.MedicalNote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MedicalNoteMapper {

    @Mapping(target = "clinicalDayId", source = "clinicalDay.id")
    MedicalNoteResponse toResponse(MedicalNote entity);
}
