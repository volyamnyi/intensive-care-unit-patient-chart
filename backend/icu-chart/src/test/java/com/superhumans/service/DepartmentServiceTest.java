package com.superhumans.service;

import com.superhumans.dto.DepartmentPatientResponse;
import com.superhumans.dto.DepartmentStatsResponse;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.ClinicalDayStatus;
import com.superhumans.icu.entity.Episode;
import com.superhumans.icu.entity.EpisodeStatus;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.EpisodeRepository;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.repository.core.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private ClinicalDayRepository clinicalDayRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MisService misService;

    @InjectMocks
    private DepartmentService departmentService;

    private UUID departmentId;

    @BeforeEach
    void setUp() {
        departmentId = UUID.randomUUID();
    }

    @Test
    void getStats_withDepartmentId_countsScopedEpisodes() {
        when(episodeRepository.countByDepartmentIdAndStatus(departmentId, EpisodeStatus.ACTIVE))
                .thenReturn(5L);
        when(clinicalDayRepository.countByStatus(ClinicalDayStatus.OPEN)).thenReturn(2L);
        when(clinicalDayRepository.countByStatus(ClinicalDayStatus.REOPENED)).thenReturn(1L);
        when(clinicalDayRepository.countByStatus(ClinicalDayStatus.NURSE_SIGNED)).thenReturn(3L);
        when(clinicalDayRepository.countByStatus(ClinicalDayStatus.DOCTOR_SIGNED)).thenReturn(4L);
        when(clinicalDayRepository.countByStatus(ClinicalDayStatus.CLOSED)).thenReturn(6L);
        when(userRepository.findByRole(UserRole.DOCTOR)).thenReturn(List.of(user(1L, "Доктор 1"), user(2L, "Доктор 2")));
        when(userRepository.findByRole(UserRole.HEAD_OF_DEPARTMENT)).thenReturn(List.of(user(3L, "Завідуючий")));
        when(userRepository.findByRole(UserRole.NURSE)).thenReturn(List.of(user(4L, "Сестра 1"), user(5L, "Сестра 2"), user(6L, "Сестра 3")));

        DepartmentStatsResponse res = departmentService.getStats(departmentId);

        assertThat(res.getActivePatients()).isEqualTo(5);
        // OPEN + REOPENED are both counted as open days
        assertThat(res.getOpenDays()).isEqualTo(3);
        assertThat(res.getNurseSignedDays()).isEqualTo(3);
        assertThat(res.getDoctorSignedDays()).isEqualTo(4);
        assertThat(res.getClosedDays()).isEqualTo(6);
        assertThat(res.getTotalBeds()).isEqualTo(12);
        assertThat(res.getOccupiedBeds()).isEqualTo(5);
        assertThat(res.getActiveDoctors()).isEqualTo(3);
        assertThat(res.getActiveNurses()).isEqualTo(3);
        verify(episodeRepository, never()).countByStatus(EpisodeStatus.ACTIVE);
    }

    @Test
    void getStats_withoutDepartmentId_countsAllEpisodes() {
        when(episodeRepository.countByStatus(EpisodeStatus.ACTIVE)).thenReturn(9L);
        when(clinicalDayRepository.countByStatus(ClinicalDayStatus.OPEN)).thenReturn(0L);
        when(clinicalDayRepository.countByStatus(ClinicalDayStatus.REOPENED)).thenReturn(0L);
        when(clinicalDayRepository.countByStatus(ClinicalDayStatus.NURSE_SIGNED)).thenReturn(0L);
        when(clinicalDayRepository.countByStatus(ClinicalDayStatus.DOCTOR_SIGNED)).thenReturn(0L);
        when(clinicalDayRepository.countByStatus(ClinicalDayStatus.CLOSED)).thenReturn(0L);
        when(userRepository.findByRole(UserRole.DOCTOR)).thenReturn(List.of());
        when(userRepository.findByRole(UserRole.HEAD_OF_DEPARTMENT)).thenReturn(List.of());
        when(userRepository.findByRole(UserRole.NURSE)).thenReturn(List.of());

        DepartmentStatsResponse res = departmentService.getStats(null);

        assertThat(res.getActivePatients()).isEqualTo(9);
        assertThat(res.getOccupiedBeds()).isEqualTo(9);
        verify(episodeRepository, never()).countByDepartmentIdAndStatus(any(), any());
    }

    @Test
    void getPatients_mapsLatestDayAndDoctorNames() {
        Episode episode = episode(UUID.randomUUID(), 1001L, 11L, LocalDateTime.now().minusDays(3));
        ClinicalDay latestDay = ClinicalDay.builder()
                .dayNumber(2)
                .status(ClinicalDayStatus.NURSE_SIGNED)
                .build();

        when(episodeRepository.findAllActive()).thenReturn(List.of(episode));
        when(userRepository.findByRole(UserRole.DOCTOR))
                .thenReturn(List.of(user(11L, "Доктор Іван"), user(12L, "Доктор Петро")));
        when(userRepository.findByRole(UserRole.HEAD_OF_DEPARTMENT)).thenReturn(List.of());
        when(misService.getPatient(1001L))
                .thenReturn(Optional.of(PatientDTO.builder().fullName("Петренко Іван Сергійович").build()));
        when(clinicalDayRepository.findFirstByEpisodeIdOrderByDayNumberDesc(episode.getId()))
                .thenReturn(Optional.of(latestDay));

        List<DepartmentPatientResponse> res = departmentService.getPatients(null);

        assertThat(res).hasSize(1);
        DepartmentPatientResponse row = res.get(0);
        assertThat(row.getPatientId()).isEqualTo(1001L);
        assertThat(row.getPatientName()).isEqualTo("Петренко Іван Сергійович");
        assertThat(row.getAttendingDoctorId()).isEqualTo(11L);
        assertThat(row.getAttendingDoctorName()).isEqualTo("Доктор Іван");
        assertThat(row.getLatestDayStatus()).isEqualTo(ClinicalDayStatus.NURSE_SIGNED);
        assertThat(row.getLatestDayNumber()).isEqualTo(2);
        assertThat(row.getDaysSinceAdmission()).isEqualTo(3);
        assertThat(row.getStatus()).isEqualTo(EpisodeStatus.ACTIVE);
    }

    @Test
    void getPatients_withDepartmentId_filtersEpisodes() {
        Episode episode = episode(UUID.randomUUID(), 1002L, null, LocalDateTime.now().minusDays(1));

        when(episodeRepository.findAllActiveByDepartmentId(departmentId)).thenReturn(List.of(episode));
        when(userRepository.findByRole(UserRole.DOCTOR)).thenReturn(List.of());
        when(userRepository.findByRole(UserRole.HEAD_OF_DEPARTMENT)).thenReturn(List.of());
        when(misService.getPatient(1002L)).thenReturn(Optional.of(PatientDTO.builder().fullName("Коваленко Олена").build()));
        when(clinicalDayRepository.findFirstByEpisodeIdOrderByDayNumberDesc(episode.getId()))
                .thenReturn(Optional.empty());

        List<DepartmentPatientResponse> res = departmentService.getPatients(departmentId);

        assertThat(res).hasSize(1);
        DepartmentPatientResponse row = res.get(0);
        assertThat(row.getLatestDayStatus()).isNull();
        assertThat(row.getLatestDayNumber()).isNull();
        assertThat(row.getAttendingDoctorName()).isNull();
        assertThat(row.getPatientName()).isEqualTo("Коваленко Олена");
        verify(episodeRepository, never()).findAllActive();
    }

    @Test
    void getPatients_whenMisPatientMissing_usesNullName() {
        Episode episode = episode(UUID.randomUUID(), 1003L, null, LocalDateTime.now());

        when(episodeRepository.findAllActive()).thenReturn(List.of(episode));
        when(userRepository.findByRole(UserRole.DOCTOR)).thenReturn(List.of());
        when(userRepository.findByRole(UserRole.HEAD_OF_DEPARTMENT)).thenReturn(List.of());
        when(misService.getPatient(1003L)).thenReturn(Optional.empty());
        when(clinicalDayRepository.findFirstByEpisodeIdOrderByDayNumberDesc(episode.getId()))
                .thenReturn(Optional.empty());

        List<DepartmentPatientResponse> res = departmentService.getPatients(null);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getPatientName()).isNull();
        assertThat(res.get(0).getDaysSinceAdmission()).isZero();
    }

    private User user(Long id, String fullName) {
        return User.builder()
                .id(id)
                .fullName(fullName)
                .build();
    }

    private Episode episode(UUID id, Long patientId, Long attendingDoctorId, LocalDateTime admissionDate) {
        Episode episode = Episode.builder()
                .patientId(patientId)
                .attendingDoctorId(attendingDoctorId)
                .admissionDate(admissionDate)
                .status(EpisodeStatus.ACTIVE)
                .departmentId(departmentId)
                .ward("1")
                .bedNumber("2")
                .admissionDiagnosis("Тестовий діагноз")
                .build();
        episode.setId(id);
        return episode;
    }
}