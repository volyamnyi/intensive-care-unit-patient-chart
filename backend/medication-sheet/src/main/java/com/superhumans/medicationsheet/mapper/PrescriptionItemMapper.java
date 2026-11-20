package com.superhumans.medicationsheet.mapper;

import com.superhumans.medicationsheet.dto.PrescriptionDayPartNested;
import com.superhumans.medicationsheet.dto.PrescriptionItemAddRequest;
import com.superhumans.medicationsheet.dto.PrescriptionItemResponse;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrescriptionItemMapper {

    @Mapping(target = "listId", source = "list.id")
    @Mapping(target = "dayParts", ignore = true)
    PrescriptionItemResponse toResponse(PrescriptionItem entity);

    @AfterMapping
    default void mapDayParts(PrescriptionItem entity, @MappingTarget PrescriptionItemResponse response) {
        mapDayParts(entity, response, Map.of());
    }

    /**
     * Overload for the grid read (#443): attaches the latest execution per day
     * part (batch-fetched by the caller — no N+1). Parts without an execution
     * keep null facts and {@code administeredAtManual = false}.
     */
    default void mapDayParts(PrescriptionItem entity, @MappingTarget PrescriptionItemResponse response,
            Map<UUID, PrescriptionExecution> executionsByDayPartId) {
        List<PrescriptionDayPartNested> result = new ArrayList<>();
        if (entity.getDays() != null) {
            for (PrescriptionItemDay day : entity.getDays()) {
                if (Boolean.TRUE.equals(day.getDeleted())) {
                    continue;
                }
                if (day.getDayParts() != null) {
                    for (PrescriptionDayPart part : day.getDayParts()) {
                        if (Boolean.TRUE.equals(part.getDeleted())) {
                            continue;
                        }
                        PrescriptionExecution execution = part.getId() == null
                                ? null
                                : executionsByDayPartId.get(part.getId());
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
                                .executionId(execution == null ? null : execution.getId())
                                .administeredAt(execution == null
                                        ? null
                                        : PrescriptionDayPartMapper.formatActualTime(execution.getAdministeredAt()))
                                .dispensedAt(execution == null
                                        ? null
                                        : PrescriptionDayPartMapper.formatActualTime(execution.getDispensedAt()))
                                .administeredAtManual(execution == null
                                        ? Boolean.FALSE
                                        : Boolean.TRUE.equals(execution.getAdministeredAtManual()))
                                .executionVersion(execution == null ? null : execution.getVersion())
                                .build());
                    }
                }
            }
        }
        response.setDayParts(result);
    }

    PrescriptionItem toEntity(PrescriptionItemAddRequest request);
}
