package com.superhumans.medicationsheet.service;

import com.superhumans.medicationsheet.entity.*;
import com.superhumans.medicationsheet.repository.*;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VitalSignServiceTest {

    @Mock private VitalSignListRepository vitalListRepository;
    @Mock private VitalSignDayRepository vitalDayRepository;
    @Mock private VitalSignEntryRepository vitalEntryRepository;
    @Mock private PrescriptionListRepository listRepository;
    @Mock private com.superhumans.audit.DomainAuditEmitter auditEmitter;
    // #382: provisioning runs inside a TransactionTemplate the service builds
    // from the med-leg manager. A stub manager that hands back a status and
    // accepts commit/rollback makes that template run the callback in-line -
    // the repositories are still pure mocks, so nothing needs a real database.
    @Mock private org.springframework.transaction.PlatformTransactionManager medTransactionManager;

    @InjectMocks
    private VitalSignService service;

    @Captor private ArgumentCaptor<VitalSignEntry> entryCaptor;
    @SuppressWarnings("unchecked")
    @Captor private ArgumentCaptor<List<VitalSignDay>> dayBatch = ArgumentCaptor.forClass(List.class);
    @SuppressWarnings("unchecked")
    @Captor private ArgumentCaptor<List<VitalSignEntry>> entryBatch = ArgumentCaptor.forClass(List.class);
    @Captor private ArgumentCaptor<VitalSignList> listCaptor;

    private UUID prescriptionListId;
    private UUID vitalListId;
    private PrescriptionList prescriptionList;
    private VitalSignList vitalList;

    @BeforeEach
    void setUp() {
        var status = new org.springframework.transaction.support.SimpleTransactionStatus();
        org.mockito.Mockito.lenient()
                .when(medTransactionManager.getTransaction(any(org.springframework.transaction.TransactionDefinition.class)))
                .thenReturn(status);
        org.mockito.Mockito.lenient().doNothing().when(medTransactionManager).commit(any());
        org.mockito.Mockito.lenient()
                .doNothing().when(medTransactionManager).rollback(any());

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
    void getOrCreate_createsNew_with21DaysAnd4EntriesEach() {
        when(vitalListRepository.findByPrescriptionListId(prescriptionListId))
                .thenReturn(Optional.empty());
        when(listRepository.findById(prescriptionListId)).thenReturn(Optional.of(prescriptionList));
when(vitalListRepository.save(any(VitalSignList.class))).thenReturn(vitalList);
        when(vitalDayRepository.saveAll(anyList())).thenAnswer(inv -> {
            java.util.List<VitalSignDay> days = new java.util.ArrayList<>(inv.getArgument(0));
            for (VitalSignDay d : days) {
                d.setId(UUID.randomUUID());
            }
            return days;
        });
        when(vitalEntryRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        VitalSignList result = service.getOrCreate(prescriptionListId);

        // 1 vital list saved
        verify(vitalListRepository).save(listCaptor.capture());
        assertThat(listCaptor.getValue().getPrescriptionList().getId()).isEqualTo(prescriptionListId);

        // 21 days created in ONE batch statement (#429 item 3)
        verify(vitalDayRepository).saveAll(dayBatch.capture());
        assertThat(dayBatch.getValue()).hasSize(21);

        // 84 entries created (21 days × 4 periods) in ONE batch statement
        verify(vitalEntryRepository).saveAll(entryBatch.capture());
        assertThat(entryBatch.getValue()).hasSize(84);

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

    // --- saveEntry (#406 idempotent slot write) ---

    @Test
    void saveEntry_writesIntoGivenSlot() {
        UUID slotDayId = UUID.randomUUID();
        VitalSignDay day = VitalSignDay.builder().vitalList(vitalList).build();
        day.setId(slotDayId);

        VitalSignEntry existing = VitalSignEntry.builder()
                .day(day).period("morning").build();
        existing.setId(UUID.randomUUID());

        when(vitalEntryRepository.findByDayIdAndPeriod(slotDayId, "morning"))
                .thenReturn(Optional.of(existing));
        when(vitalEntryRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(vitalEntryRepository.save(any())).thenReturn(existing);

        VitalSignEntry update = new VitalSignEntry();
        update.setTemperature(37.0);
        update.setPulse(75);

        VitalSignEntry result = service.saveEntry(slotDayId, "morning", update);

        assertThat(result).isSameAs(existing);
        verify(vitalEntryRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getTemperature()).isEqualTo(37.0);
        assertThat(entryCaptor.getValue().getPulse()).isEqualTo(75);
    }

    @Test
    void saveEntry_createsMissingSlotThenWrites() {
        UUID slotDayId = UUID.randomUUID();
        VitalSignDay day = VitalSignDay.builder().vitalList(vitalList).build();
        day.setId(slotDayId);

        VitalSignEntry created = VitalSignEntry.builder()
                .day(day).period("evening").build();
        created.setId(UUID.randomUUID());

        when(vitalDayRepository.findById(slotDayId)).thenReturn(Optional.of(day));
        when(vitalEntryRepository.findByDayIdAndPeriod(slotDayId, "evening"))
                .thenReturn(Optional.empty());
        when(vitalEntryRepository.save(any())).thenReturn(created);
        when(vitalEntryRepository.findById(created.getId())).thenReturn(Optional.of(created));

        VitalSignEntry update = new VitalSignEntry();
        update.setPulse(60);

        VitalSignEntry result = service.saveEntry(slotDayId, "evening", update);

        assertThat(result).isSameAs(created);
        verify(vitalEntryRepository, times(2)).save(any());
    }
}