package com.superhumans.mis;

import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the canonical MIS sex-code contract: the in-memory mock must serve the
 * same codes as the wiremock fixtures (MAL/FEM) so that consumers that map
 * sex codes to labels (e.g. {@code ProstheticsPatientService}) never receive
 * legacy single-letter codes.
 */
@ExtendWith(MockitoExtension.class)
class MockMisServiceImplTest {

    @Mock
    AuditService auditService;

    MockMisServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MockMisServiceImpl(auditService);
        service.init();
    }

    @Test
    void allPatientsUseCanonicalMisSexCodes() {
        List<PatientDTO> patients = service.searchPatients(null);

        assertThat(patients).isNotEmpty();
        assertThat(patients)
                .allSatisfy(p -> assertThat(p.getSexCode()).isIn("MAL", "FEM"));
    }

    @Test
    void canonicalPatientsUseFullMatchingDemographics() {
        // The mock and the wiremock fixtures must describe the same patients;
        // spot-check the canonical ICU seed patient used across tests and E2E.
        PatientDTO petrenko = service.getPatient(1001L).orElseThrow();
        assertThat(petrenko.getFullName()).isEqualTo("Петренко Іван Сергійович");
        assertThat(petrenko.getSexCode()).isEqualTo("MAL");
    }
}
