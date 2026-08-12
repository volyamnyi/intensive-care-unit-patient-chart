package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsPatientResponse;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsPatientMapper;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the patient registry facade contract: the MIS Integration Layer is the
 * single source of truth for patient demographics; the local registry only
 * contributes prosthesis-specific clinical fields. Sex codes from MIS (MAL/FEM,
 * per the wiremock fixtures in the common module) map to UI labels.
 */
@ExtendWith(MockitoExtension.class)
class ProstheticsPatientServiceTest {

    @Mock
    MisService misService;
    @Mock
    ProstheticsPatientRepository patientRepository;
    @Mock
    ProstheticsPatientMapper patientMapper;

    ProstheticsPatientService service;

    @BeforeEach
    void setUp() {
        service = new ProstheticsPatientService(misService, patientRepository, patientMapper);
    }

    private PatientDTO misPatient(String sexCode) {
        return PatientDTO.builder()
                .id(900001L)
                .fullName("Сніжко Іван Петрович")
                .birthDate(LocalDate.of(1991, 3, 14))
                .sexCode(sexCode)
                .height(182)
                .weight(84)
                .address("м. Миколаїв, вул. Чапаєва, буд. 54-А, кв. 17")
                .phone("380933329111")
                .email("snizhko.ivan@example.com")
                .build();
    }

    private ProstheticsPatient localClinicalRecord() {
        return ProstheticsPatient.builder()
                .id("900001")
                .pib("Сніжко Іван Петрович")
                .cause("Мінно-вибухова травма")
                .amputationDate(LocalDate.of(2024, 11, 8))
                .affectedLimb("RIGHT")
                .amputationLevel("upper_third_forearm")
                .stump("[{\"label\":\"Форма кукси\",\"value\":\"Циліндрична\"}]")
                .build();
    }

    @Test
    void search_usesMisDemographicsAndLocalClinicalFields() {
        when(misService.searchPatients("Сніжко")).thenReturn(List.of(misPatient("MAL")));
        when(patientRepository.findById("900001")).thenReturn(Optional.of(localClinicalRecord()));

        List<ProstheticsPatientResponse> result = service.search("Сніжко");

        assertThat(result).hasSize(1);
        ProstheticsPatientResponse r = result.get(0);
        // demographics come from MIS, never from the local registry
        assertThat(r.getPib()).isEqualTo("Сніжко Іван Петрович");
        assertThat(r.getBirthDate()).isEqualTo(LocalDate.of(1991, 3, 14));
        assertThat(r.getGender()).isEqualTo("Чоловіча");
        assertThat(r.getHeightCm()).isEqualTo(182);
        assertThat(r.getWeightKg()).isEqualTo(84);
        assertThat(r.getResidence()).isEqualTo("м. Миколаїв, вул. Чапаєва, буд. 54-А, кв. 17");
        // clinical fields come from the local registry
        assertThat(r.getCause()).isEqualTo("Мінно-вибухова травма");
        assertThat(r.getAmputationDate()).isEqualTo(LocalDate.of(2024, 11, 8));
        assertThat(r.getAmputationLevel()).isEqualTo("upper_third_forearm");
        assertThat(r.getStump()).isEqualTo("[{\"label\":\"Форма кукси\",\"value\":\"Циліндрична\"}]");

        verify(patientRepository).findById("900001");
        verify(patientMapper, never()).toResponse(any());
    }

    @Test
    void search_mapsFemSexCodeToZhinocha() {
        when(misService.searchPatients("Гаврилюк")).thenReturn(List.of(misPatient("FEM")));
        when(patientRepository.findById("900001")).thenReturn(Optional.empty());

        List<ProstheticsPatientResponse> result = service.search("Гаврилюк");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGender()).isEqualTo("Жіноча");
    }

    @Test
    void search_fallsBackToLocalRegistryOnMisFailure() {
        when(misService.searchPatients("Сніжко")).thenThrow(new RuntimeException("MIS timeout"));
        ProstheticsPatient local = localClinicalRecord();
        when(patientRepository.findByPibContainingIgnoreCase("Сніжко")).thenReturn(List.of(local));
        ProstheticsPatientResponse localResponse = ProstheticsPatientResponse.builder()
                .id("900001").pib("Сніжко Іван Петрович").cause("Мінно-вибухова травма").build();
        when(patientMapper.toResponse(local)).thenReturn(localResponse);

        List<ProstheticsPatientResponse> result = service.search("Сніжко");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPib()).isEqualTo("Сніжко Іван Петрович");
        verify(patientMapper).toResponse(local);
    }

    @Test
    void get_mergesMisWithLocalClinicalFields() {
        when(misService.getPatient(900001L)).thenReturn(Optional.of(misPatient("MAL")));
        when(patientRepository.findById("900001")).thenReturn(Optional.of(localClinicalRecord()));

        ProstheticsPatientResponse r = service.get("900001");

        assertThat(r.getPib()).isEqualTo("Сніжко Іван Петрович");
        assertThat(r.getGender()).isEqualTo("Чоловіча");
        assertThat(r.getCause()).isEqualTo("Мінно-вибухова травма");
    }

    @Test
    void get_fallsBackToLocalRegistryWhenMisHasNoPatient() {
        when(misService.getPatient(900001L)).thenReturn(Optional.empty());
        ProstheticsPatient local = localClinicalRecord();
        when(patientRepository.findById("900001")).thenReturn(Optional.of(local));
        ProstheticsPatientResponse localResponse = ProstheticsPatientResponse.builder()
                .id("900001").pib("Сніжко Іван Петрович").build();
        when(patientMapper.toResponse(local)).thenReturn(localResponse);

        ProstheticsPatientResponse r = service.get("900001");

        assertThat(r.getPib()).isEqualTo("Сніжко Іван Петрович");
        verify(patientMapper).toResponse(local);
    }

    @Test
    void get_fallsBackToLocalRegistryWhenMisThrows() {
        when(misService.getPatient(900001L)).thenThrow(new RuntimeException("MIS unavailable"));
        ProstheticsPatient local = localClinicalRecord();
        when(patientRepository.findById("900001")).thenReturn(Optional.of(local));
        when(patientMapper.toResponse(local)).thenReturn(
                ProstheticsPatientResponse.builder().id("900001").pib("Сніжко Іван Петрович").build());

        assertThat(service.get("900001").getPib()).isEqualTo("Сніжко Іван Петрович");
    }

    @Test
    void get_throwsNotFoundWhenPatientUnknownEverywhere() {
        when(misService.getPatient(999999L)).thenReturn(Optional.empty());
        when(patientRepository.findById("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("999999")).isInstanceOf(NotFoundException.class);
    }
}
