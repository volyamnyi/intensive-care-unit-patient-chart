package com.superhumans.medicationsheet.mapper;

import com.superhumans.medicationsheet.dto.PrescriptionDayPartNested;
import com.superhumans.medicationsheet.dto.PrescriptionItemResponse;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionItemMapperTest {

    private final PrescriptionItemMapper mapper = new PrescriptionItemMapperImpl();

    @Test
    void toResponse_mapsNestedDayParts() {
        var list = PrescriptionList.builder().patientId(1001L).build();
        list.setId(UUID.randomUUID());

        var day1 = PrescriptionItemDay.builder().dayDate(LocalDate.of(2026, 7, 26)).build();
        day1.setId(UUID.randomUUID());

        var part1 = PrescriptionDayPart.builder()
                .day(day1).period("morning").dose("500mg")
                .isPlanned(true).isCompleted(false)
                .doctorName("Dr. Test").build();
        part1.setId(UUID.randomUUID());

        var part2 = PrescriptionDayPart.builder()
                .day(day1).period("day").dose("250mg")
                .isPlanned(true).isCompleted(true)
                .nurseName("Nurse Test").build();
        part2.setId(UUID.randomUUID());

        day1.setDayParts(List.of(part1, part2));

        var item = PrescriptionItem.builder()
                .list(list).medicineName("Aspirin").medicineMethod("PO")
                .regime("BID").status("Active").sortOrder(0).build();
        item.setId(UUID.randomUUID());
        item.setDays(List.of(day1));

        PrescriptionItemResponse response = mapper.toResponse(item);

        assertThat(response.getMedicineName()).isEqualTo("Aspirin");
        assertThat(response.getListId()).isEqualTo(list.getId());
        assertThat(response.getDayParts()).hasSize(2);

        PrescriptionDayPartNested dp1 = response.getDayParts().get(0);
        assertThat(dp1.getDayDate()).isEqualTo(LocalDate.of(2026, 7, 26));
        assertThat(dp1.getPeriod()).isEqualTo("morning");
        assertThat(dp1.getDose()).isEqualTo("500mg");
        assertThat(dp1.getIsPlanned()).isTrue();
        assertThat(dp1.getDoctorName()).isEqualTo("Dr. Test");

        PrescriptionDayPartNested dp2 = response.getDayParts().get(1);
        assertThat(dp2.getPeriod()).isEqualTo("day");
        assertThat(dp2.getIsCompleted()).isTrue();
        assertThat(dp2.getNurseName()).isEqualTo("Nurse Test");
    }

    @Test
    void toResponse_handlesNullDays() {
        var list = PrescriptionList.builder().patientId(1001L).build();
        list.setId(UUID.randomUUID());

        var item = PrescriptionItem.builder()
                .list(list).medicineName("Morphine").build();
        item.setId(UUID.randomUUID());

        PrescriptionItemResponse response = mapper.toResponse(item);

        assertThat(response.getMedicineName()).isEqualTo("Morphine");
        assertThat(response.getDayParts()).isNotNull().isEmpty();
    }

    @Test
    void toResponse_handlesDayWithNullParts() {
        var list = PrescriptionList.builder().patientId(1001L).build();
        list.setId(UUID.randomUUID());

        var day = PrescriptionItemDay.builder().dayDate(LocalDate.now()).build();
        day.setId(UUID.randomUUID());

        var item = PrescriptionItem.builder()
                .list(list).medicineName("Drug").build();
        item.setId(UUID.randomUUID());
        item.setDays(List.of(day));

        PrescriptionItemResponse response = mapper.toResponse(item);

        assertThat(response.getDayParts()).isNotNull().isEmpty();
    }

    /**
     * The grid wire carries the actual-time facts of the latest execution plus
     * its optimistic-lock version (#446) - the correction endpoint needs the
     * version, otherwise a stale write would silently overwrite the fact.
     */
    @Test
    void mapDayParts_attachesExecutionFactsAndVersion() {
        var list = PrescriptionList.builder().patientId(1001L).build();
        list.setId(UUID.randomUUID());

        var day = PrescriptionItemDay.builder().dayDate(LocalDate.of(2026, 7, 26)).build();
        day.setId(UUID.randomUUID());

        var executed = PrescriptionDayPart.builder()
                .day(day).period("morning").dose("500mg")
                .isPlanned(true).isCompleted(true).build();
        executed.setId(UUID.randomUUID());

        var planned = PrescriptionDayPart.builder()
                .day(day).period("day").dose("250mg")
                .isPlanned(true).isCompleted(false).build();
        planned.setId(UUID.randomUUID());

        day.setDayParts(List.of(executed, planned));

        var item = PrescriptionItem.builder()
                .list(list).medicineName("Aspirin").status("Active").sortOrder(0).build();
        item.setId(UUID.randomUUID());
        item.setDays(List.of(day));

        var execution = PrescriptionExecution.builder()
                .dayPart(executed)
                .administeredAt(LocalDateTime.of(2026, 7, 26, 10, 30, 7))
                .dispensedAt(LocalDateTime.of(2026, 7, 26, 10, 29, 0))
                .administeredAtManual(true)
                .build();
        execution.setId(UUID.randomUUID());
        execution.setVersion(3);

        PrescriptionItemResponse response = mapper.toResponse(item);
        mapper.mapDayParts(item, response, Map.of(executed.getId(), execution));

        PrescriptionDayPartNested withExecution = response.getDayParts().get(0);
        assertThat(withExecution.getExecutionId()).isEqualTo(execution.getId());
        assertThat(withExecution.getAdministeredAt()).isEqualTo("2026-07-26T10:30:07");
        assertThat(withExecution.getDispensedAt()).isEqualTo("2026-07-26T10:29:00");
        assertThat(withExecution.getAdministeredAtManual()).isTrue();
        assertThat(withExecution.getExecutionVersion()).isEqualTo(3);

        PrescriptionDayPartNested withoutExecution = response.getDayParts().get(1);
        assertThat(withoutExecution.getExecutionId()).isNull();
        assertThat(withoutExecution.getAdministeredAt()).isNull();
        assertThat(withoutExecution.getDispensedAt()).isNull();
        assertThat(withoutExecution.getAdministeredAtManual()).isFalse();
        assertThat(withoutExecution.getExecutionVersion()).isNull();
    }
}
