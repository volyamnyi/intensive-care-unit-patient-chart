package com.superhumans.service;

import com.superhumans.dto.DepartmentPatientResponse;
import com.superhumans.dto.DepartmentStatsResponse;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.Episode;
import com.superhumans.entity.EpisodeStatus;
import com.superhumans.entity.UserRole;
import com.superhumans.mis.MisService;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.EpisodeRepository;
import com.superhumans.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepartmentService {

    EpisodeRepository episodeRepository;
    ClinicalDayRepository clinicalDayRepository;
    UserRepository userRepository;
    MisService misService;

    public DepartmentStatsResponse getStats() {
        long activePatients = episodeRepository.countByStatus(EpisodeStatus.ACTIVE);
        long openDays = clinicalDayRepository.countByStatus(ClinicalDayStatus.OPEN)
                + clinicalDayRepository.countByStatus(ClinicalDayStatus.REOPENED);
        long nurseSignedDays = clinicalDayRepository.countByStatus(ClinicalDayStatus.NURSE_SIGNED);
        long doctorSignedDays = clinicalDayRepository.countByStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        long closedDays = clinicalDayRepository.countByStatus(ClinicalDayStatus.CLOSED);
        long occupiedBeds = episodeRepository.countByStatus(EpisodeStatus.ACTIVE);
        long activeDoctors = userRepository.findByRole(UserRole.DOCTOR).size()
                + userRepository.findByRole(UserRole.HEAD_OF_DEPARTMENT).size();
        long activeNurses = userRepository.findByRole(UserRole.NURSE).size();

        return DepartmentStatsResponse.builder()
                .activePatients(activePatients)
                .openDays(openDays)
                .nurseSignedDays(nurseSignedDays)
                .doctorSignedDays(doctorSignedDays)
                .closedDays(closedDays)
                .totalBeds(12)
                .occupiedBeds(occupiedBeds)
                .activeDoctors(activeDoctors)
                .activeNurses(activeNurses)
                .build();
    }

    public List<DepartmentPatientResponse> getPatients() {
        List<Episode> activeEpisodes = episodeRepository.findAllActive();

        Map<Long, String> doctorNames = userRepository.findByRole(UserRole.DOCTOR).stream()
                .collect(Collectors.toMap(
                    u -> u.getId(),
                    u -> u.getFullName(),
                    (a, b) -> a
                ));
        userRepository.findByRole(UserRole.HEAD_OF_DEPARTMENT).forEach(
                u -> doctorNames.put(u.getId(), u.getFullName()));

        return activeEpisodes.stream().map(ep -> {
            String patientName = misService.getPatient(ep.getPatientId())
                    .map(p -> p.getFullName()).orElse(null);

            var latestDay = clinicalDayRepository
                    .findFirstByEpisodeIdOrderByDayNumberDesc(ep.getId());

            ClinicalDayStatus dayStatus = latestDay.map(d -> d.getStatus()).orElse(null);
            Integer dayNumber = latestDay.map(d -> d.getDayNumber()).orElse(null);

            long daysSince = ChronoUnit.DAYS.between(
                    ep.getAdmissionDate().toLocalDate(), LocalDate.now());

            String doctorName = ep.getAttendingDoctorId() != null
                    ? doctorNames.get(ep.getAttendingDoctorId())
                    : null;

            return DepartmentPatientResponse.builder()
                    .id(ep.getId())
                    .patientId(ep.getPatientId())
                    .patientName(patientName)
                    .hospitalizationId(ep.getHospitalizationId())
                    .departmentId(ep.getDepartmentId())
                    .admissionDate(ep.getAdmissionDate())
                    .dischargeDate(ep.getDischargeDate())
                    .status(ep.getStatus())
                    .attendingDoctorId(ep.getAttendingDoctorId())
                    .attendingDoctorName(doctorName)
                    .ward(ep.getWard())
                    .bedNumber(ep.getBedNumber())
                    .admissionDiagnosis(ep.getAdmissionDiagnosis())
                    .latestDayStatus(dayStatus)
                    .latestDayNumber(dayNumber)
                    .daysSinceAdmission((int) daysSince)
                    .build();
        }).collect(Collectors.toList());
    }
}
