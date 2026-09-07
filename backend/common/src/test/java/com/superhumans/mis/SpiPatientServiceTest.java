package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.service.AuditService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit coverage for the {@code spiPatientProsthesCheck} seam (#256): real mode
 * calls the SPI procedure and maps all 12 patient fields, wiremock mode keeps
 * the legacy procedure, and {@code searchPatients}/{@code getPatient} delegate
 * to {@code getAllPatientsUnderTreatment} in real mode.
 */
@ExtendWith(MockitoExtension.class)
class SpiPatientServiceTest {

    private static final String PATIENTS_JSON = """
            {"patientList":[
              {"patientID":1001,"patientName":"Петренко Іван Сергійович",
               "patientBirthDate":"1978-03-15","patientSexCode":"MAL",
               "patientAddress":"м. Київ","patientPhone":"380501234567",
               "patientEmail":"ivan.petrenko@mail.com",
               "patientExternalID1":"КВ-001234","patientExternalID2":"301020251234",
               "patientHeight":178,"patientWeight":82,
               "patientBloodGroup":"A(II)","patientRhFactor":"Rh+",
               "patientRoomNumber":"Палата 501","patientBedNumber":"Ліжко-1",
               "patientDoctor":"Олександр Мельник","patientDepartmentID":19},
              {"patientID":1002,"patientName":"Коваленко Олена Вікторівна",
               "patientBirthDate":"1985-11-22","patientSexCode":"FEM",
               "patientAddress":"м. Львів","patientPhone":"380671112233",
               "patientEmail":"olena.kov@mail.com",
               "patientExternalID1":"КВ-005678","patientExternalID2":"150320259876",
               "patientHeight":165,"patientWeight":58,
               "patientBloodGroup":"B(III)","patientRhFactor":"Rh+",
               "patientRoomNumber":"Палата 502","patientBedNumber":"Ліжко-2",
               "patientDoctor":"Ольга Бойко","patientDepartmentID":27}
            ]}""";

    @Mock
    MisApiClient misApiClient;

    @Mock
    AuditService auditService;

    final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode json(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private WireMockMisServiceImpl realModeService() {
        MisApiProperties properties = new MisApiProperties();
        ReflectionTestUtils.setField(properties, "mode", "real");
        return new WireMockMisServiceImpl(misApiClient, auditService, properties);
    }

    private WireMockMisServiceImpl wiremockModeService() {
        return new WireMockMisServiceImpl(misApiClient, auditService, new MisApiProperties());
    }

    @Test
    void realMode_getAllPatients_callsSpiProcedure() {
        when(misApiClient.callMethod(anyString())).thenReturn(json(PATIENTS_JSON));

        List<PatientDTO> result = realModeService().getAllPatientsUnderTreatment();

        verify(misApiClient).callMethod(
                WireMockMisServiceImpl.SPI_PATIENT_PROCEDURE);
        assertThat(result).hasSize(2);
    }

    @Test
    void realMode_mapsAllTwelveFields() {
        when(misApiClient.callMethod(anyString())).thenReturn(json(PATIENTS_JSON));

        PatientDTO patient = realModeService().getAllPatientsUnderTreatment().get(0);

        assertThat(patient.getId()).isEqualTo(1001L);
        assertThat(patient.getFullName()).isEqualTo("Петренко Іван Сергійович");
        assertThat(patient.getBirthDate()).hasToString("1978-03-15");
        assertThat(patient.getSexCode()).isEqualTo("MAL");
        assertThat(patient.getAddress()).isEqualTo("м. Київ");
        assertThat(patient.getPhone()).isEqualTo("380501234567");
        assertThat(patient.getEmail()).isEqualTo("ivan.petrenko@mail.com");
        assertThat(patient.getBloodGroup()).isEqualTo("A(II)");
        assertThat(patient.getRhFactor()).isEqualTo("Rh+");
        assertThat(patient.getDepartmentId()).isEqualTo(19L);
        assertThat(patient.getRoom()).isEqualTo("Палата 501");
        assertThat(patient.getBed()).isEqualTo("Ліжко-1");
        assertThat(patient.getDoctorName()).isEqualTo("Олександр Мельник");
    }

    @Test
    void realMode_searchPatients_filtersWithoutLegacyCall() {
        when(misApiClient.callMethod(anyString())).thenReturn(json(PATIENTS_JSON));

        List<PatientDTO> result = realModeService().searchPatients("коваленко");

        verify(misApiClient).callMethod(
                WireMockMisServiceImpl.SPI_PATIENT_PROCEDURE);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1002L);
    }

    @Test
    void realMode_getPatient_findsByIdWithoutLegacyCall() {
        when(misApiClient.callMethod(anyString())).thenReturn(json(PATIENTS_JSON));

        Optional<PatientDTO> result = realModeService().getPatient(1002L);

        verify(misApiClient).callMethod(
                WireMockMisServiceImpl.SPI_PATIENT_PROCEDURE);
        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).contains("Коваленко");
    }

    @Test
    void realMode_emptyResponse_returnsEmptyList() {
        when(misApiClient.callMethod(anyString())).thenReturn(json("{}"));

        assertThat(realModeService().getAllPatientsUnderTreatment()).isEmpty();
        assertThat(realModeService().searchPatients("x")).isEmpty();
        assertThat(realModeService().getPatient(1001L)).isEmpty();
    }

    @Test
    void wiremockMode_getAllPatients_callsLegacyProcedure() {
        when(misApiClient.callMethod(anyString())).thenReturn(json(PATIENTS_JSON));

        List<PatientDTO> result = wiremockModeService().getAllPatientsUnderTreatment();

        verify(misApiClient).callMethod("spzIBPatientSearch");
        assertThat(result).hasSize(2);
    }

    @Test
    void wiremockMode_searchPatients_keepsLegacyCall() {
        when(misApiClient.callMethod(anyString())).thenReturn(json(PATIENTS_JSON));

        List<PatientDTO> result = wiremockModeService().searchPatients("петренко");

        verify(misApiClient).callMethod("spzIBPatientSearch");
        assertThat(result).hasSize(1);
    }
}
