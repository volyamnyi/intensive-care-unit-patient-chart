package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VitalSignServiceTest {

    @Mock private VitalSignListRepository vitalListRepository;
    @Mock private VitalSignDayRepository vitalDayRepository;
    @Mock private VitalSignEntryRepository vitalEntryRepository;
    @Mock private PrescriptionListRepository listRepository;

    @InjectMocks
    private VitalSignService service;

    @Captor private ArgumentCaptor<VitalSignEntry> entryCaptor;
    @Captor private ArgumentCaptor<VitalSignList> listCaptor;

    private UUID prescriptionListId;
    private UUID vitalListId;
    private PrescriptionList prescriptionList;
    private VitalSignList vitalList;

    @BeforeEach
    void setUp() {
        prescriptionListId = UUID.randomUUID();
        vitalListId = UUID.randomUUID();

        prescriptionList = PrescriptionList.builder()
                .patientId(1001L).status("Saved").build();
        prescriptionList.setId(prescriptionListId);

        vitalList = VitalSignList.builder()
                .prescriptionList(prescriptionList).build();
        vitalList.setId(vitalListId);
    }

    // --- getOrCreate ---

    @Test
    void getOrCreate_returnsExisting_whenFound() {
        when(vitalListRepository.findByPrescriptionListId(prescriptionListId))
                .thenReturn(Optional.of(vitalList));

        VitalSignList result = service.getOrCreate(prescriptionListId);

        assertThat(result.getId()).isEqualTo(vitalListId);
        verify(vitalListRepository, never()).save(any());
    }

    @Test
    void getOrCreate_createsNew_with21DaysAnd2EntriesEach() {
        when(vitalListRepository.findByPrescriptionListId(prescriptionListId))
                .thenReturn(Optional.empty());
        when(listRepository.findById(prescriptionListId)).thenReturn(Optional.of(prescriptionList));
        when(vitalListRepository.save(any(VitalSignList.class))).thenReturn(vitalList);
        when(vitalDayRepository.save(any(VitalSignDay.class))).thenAnswer(inv -> {
            VitalSignDay d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(vitalEntryRepository.save(any(VitalSignEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        VitalSignList result = service.getOrCreate(prescriptionListId);

        // 1 vital list saved
        verify(vitalListRepository).save(listCaptor.capture());
        assertThat(listCaptor.getValue().getPrescriptionList().getId()).isEqualTo(prescriptionListId);

        // 21 days created
        verify(vitalDayRepository, times(21)).save(any(VitalSignDay.class));

        // 42 entries created (21 days × 2 periods: morning, evening)
        verify(vitalEntryRepository, times(42)).save(any(VitalSignEntry.class));

        assertThat(result.getId()).isEqualTo(vitalListId);
    }

    @Test
    void getOrCreate_throws_whenPrescriptionListNotFound() {
        when(vitalListRepository.findByPrescriptionListId(any())).thenReturn(Optional.empty());
        when(listRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrCreate(UUID.randomUUID()))
                .isInstanceOf(Exception.class);
    }

    // --- getDays ---

    @Test
    void getDays_returnsDays() {
        VitalSignDay day = VitalSignDay.builder()
                .vitalList(vitalList).dayDate(java.time.LocalDate.now()).build();
        day.setId(UUID.randomUUID());
        when(vitalDayRepository.findByVitalListIdOrderByDayDateAsc(vitalListId))
                .thenReturn(List.of(day));

        var result = service.getDays(vitalListId);

        assertThat(result).hasSize(1);
    }

    // --- getEntries ---

    @Test
    void getEntries_returnsEntriesForDay() {
        UUID dayId = UUID.randomUUID();
        VitalSignEntry entry = VitalSignEntry.builder()
                .period("morning").temperature(36.6).pulse(72).build();
        entry.setId(UUID.randomUUID());
        VitalSignDay day = VitalSignDay.builder().vitalList(vitalList).build();
        day.setId(dayId);
        entry.setDay(day);

        when(vitalEntryRepository.findByDayId(dayId)).thenReturn(List.of(entry));

        var result = service.getEntries(dayId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTemperature()).isEqualTo(36.6);
    }

    // --- updateEntry ---

    @Test
    void updateEntry_updatesVitalSignFields() {
        UUID entryId = UUID.randomUUID();
        VitalSignEntry existing = VitalSignEntry.builder()
                .period("morning").build();
        existing.setId(entryId);
        VitalSignDay day = VitalSignDay.builder().vitalList(vitalList).build();
        day.setId(UUID.randomUUID());
        existing.setDay(day);

        VitalSignEntry update = new VitalSignEntry();
        update.setTemperature(37.2);
        update.setSystolicBp(130);
        update.setDiastolicBp(85);
        update.setSpo2(97);
        update.setPulse(80);
        update.setStool("normal");
        update.setPainScore(3);

        when(vitalEntryRepository.findById(entryId)).thenReturn(Optional.of(existing));
        when(vitalEntryRepository.save(any())).thenReturn(existing);

        VitalSignEntry result = service.updateEntry(entryId, update);

        verify(vitalEntryRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getTemperature()).isEqualTo(37.2);
        assertThat(entryCaptor.getValue().getSystolicBp()).isEqualTo(130);
        assertThat(entryCaptor.getValue().getDiastolicBp()).isEqualTo(85);
        assertThat(entryCaptor.getValue().getSpo2()).isEqualTo(97);
        assertThat(entryCaptor.getValue().getPulse()).isEqualTo(80);
        assertThat(entryCaptor.getValue().getStool()).isEqualTo("normal");
        assertThat(entryCaptor.getValue().getPainScore()).isEqualTo(3);
    }

    @Test
    void updateEntry_throws_whenNotFound() {
        when(vitalEntryRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateEntry(UUID.randomUUID(), new VitalSignEntry()))
                .isInstanceOf(Exception.class);
    }

    // --- saveNextEntry ---

    @Test
    void saveNextEntry_fillsFirstEmptySlot() {
        // Set up: 1 day, 2 entries (morning is filled, evening is empty)
        VitalSignDay day = VitalSignDay.builder().vitalList(vitalList).build();
        day.setId(UUID.randomUUID());

        VitalSignEntry filledEntry = VitalSignEntry.builder()
                .day(day).period("morning").temperature(36.6).build();
        filledEntry.setId(UUID.randomUUID());

        VitalSignEntry emptyEntry = VitalSignEntry.builder()
                .day(day).period("evening").build();
        emptyEntry.setId(UUID.randomUUID());

        when(vitalListRepository.findByPrescriptionListId(prescriptionListId))
                .thenReturn(Optional.of(vitalList));
        when(vitalDayRepository.findByVitalListIdOrderByDayDateAsc(vitalListId))
                .thenReturn(List.of(day));
        when(vitalEntryRepository.findByDayId(day.getId()))
                .thenReturn(List.of(filledEntry, emptyEntry));
        when(vitalEntryRepository.findById(emptyEntry.getId())).thenReturn(Optional.of(emptyEntry));
        when(vitalEntryRepository.save(any())).thenReturn(emptyEntry);

        VitalSignEntry update = new VitalSignEntry();
        update.setTemperature(37.0);
        update.setPulse(75);

        VitalSignEntry result = service.saveNextEntry(prescriptionListId, update);

        verify(vitalEntryRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getTemperature()).isEqualTo(37.0);
        assertThat(entryCaptor.getValue().getPulse()).isEqualTo(75);
    }

    @Test
    void saveNextEntry_throws_whenNoEmptySlot() {
        VitalSignDay day = VitalSignDay.builder().vitalList(vitalList).build();
        day.setId(UUID.randomUUID());

        // All entries filled
        VitalSignEntry filled = VitalSignEntry.builder()
                .day(day).period("morning").temperature(36.6).pulse(70).build();
        filled.setId(UUID.randomUUID());

        when(vitalListRepository.findByPrescriptionListId(prescriptionListId))
                .thenReturn(Optional.of(vitalList));
        when(vitalDayRepository.findByVitalListIdOrderByDayDateAsc(vitalListId))
                .thenReturn(List.of(day));
        when(vitalEntryRepository.findByDayId(day.getId()))
                .thenReturn(List.of(filled));

        assertThatThrownBy(() -> service.saveNextEntry(prescriptionListId, new VitalSignEntry()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No empty vital sign entry slot");
    }
}
