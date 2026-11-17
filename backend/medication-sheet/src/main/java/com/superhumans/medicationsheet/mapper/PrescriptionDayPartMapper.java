package com.superhumans.medicationsheet.mapper;

import com.superhumans.medicationsheet.dto.PrescriptionDayPartResponse;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrescriptionDayPartMapper {

    /**
     * Wire always carries seconds (#443): {@code HH:mm} values normalize to
     * {@code HH:mm:00} on output, because the pattern emits seconds
     * unconditionally.
     */
    DateTimeFormatter ACTUAL_TIME_WIRE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Mapping(target = "dayId", source = "day.id")
    PrescriptionDayPartResponse toResponse(PrescriptionDayPart entity);

    /**
     * Overload for mutation endpoints (#443): attaches the latest execution's
     * actual-time facts to the day part. Null-safe for executions built
     * without the new fields — the facts stay null and the manual flag is
     * false.
     */
    default PrescriptionDayPartResponse toResponse(PrescriptionDayPart part, PrescriptionExecution execution) {
        PrescriptionDayPartResponse response = toResponse(part);
        if (execution == null) {
            return response;
        }
        response.setExecutionId(execution.getId());
        response.setAdministeredAt(formatActualTime(execution.getAdministeredAt()));
        response.setDispensedAt(formatActualTime(execution.getDispensedAt()));
        response.setAdministeredAtManual(Boolean.TRUE.equals(execution.getAdministeredAtManual()));
        response.setExecutionVersion(execution.getVersion());
        return response;
    }

    static String formatActualTime(LocalDateTime value) {
        return value == null ? null : ACTUAL_TIME_WIRE_FORMAT.format(value);
    }
}
