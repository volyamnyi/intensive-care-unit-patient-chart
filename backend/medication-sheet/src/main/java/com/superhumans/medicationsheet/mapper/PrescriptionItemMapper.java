package com.superhumans.medicationsheet.mapper;

import com.superhumans.medicationsheet.dto.PrescriptionDayPartNested;
import com.superhumans.medicationsheet.dto.PrescriptionItemAddRequest;
import com.superhumans.medicationsheet.dto.PrescriptionItemResponse;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrescriptionItemMapper {

    @Mapping(target = "listId", source = "list.id")
    @Mapping(target = "dayParts", ignore = true)
    PrescriptionItemResponse toResponse(PrescriptionItem entity);

    @AfterMapping
    default void mapDayParts(PrescriptionItem entity, @MappingTarget PrescriptionItemResponse response) {
        List<PrescriptionDayPartNested> result = new ArrayList<>();
        if (entity.getDays() != null) {
            for (PrescriptionItemDay day : entity.getDays()) {
                if (Boolean.TRUE.equals(day.getDeleted())) {
                    continue;
                }
                if (day.getDayParts() != null) {
                    for (PrescriptionDayPart part : day.getDayParts()) {
                        result.add(PrescriptionDayPartNested.builder()
                                .id(part.getId())
                                .dayId(day.getId())
                                .dayDate(day.getDayDate())
                                .period(part.getPeriod())
                                .dose(part.getDose())
                                .isPlanned(part.getIsPlanned())
                                .isPlannedFinished(part.getIsPlannedFinished())
                                .isCompleted(part.getIsCompleted())
                                .isCompletedFinished(part.getIsCompletedFinished())
                                .doctorName(part.getDoctorName())
                                .nurseName(part.getNurseName())
                                .build());
                    }
                }
            }
        }
        response.setDayParts(result);
    }

    PrescriptionItem toEntity(PrescriptionItemAddRequest request);
}
