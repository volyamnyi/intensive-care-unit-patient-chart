package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Builds {@link MisOrderTemplateData} for prosthetics order templates by calling
 * the MIS Integration Layer (common module). All data is READ-ONLY per MIS policy.
 * <p>
 * When the MIS integration is unavailable (wiremock disabled / mock mode off),
 * the methods return {@code Optional.empty()} / empty lists so templates fall back
 * to locally stored patient/order data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MisOrderTemplateDataService {

    final MisService misService;

    /**
     * Loads MIS data for a prosthetics order template.
     *
     * @param patientId MIS patient id (digits-only string, e.g. "900001")
     * @return aggregated MIS data; empty fields when MIS is unavailable
     */
    public MisOrderTemplateData load(String patientId) {
        Long misPatientId = parsePatientId(patientId);
        if (misPatientId == null) {
            return empty();
        }
        try {
            Optional<PatientInfoMisDTO> patientInfo = misService.getPatientInfo(misPatientId);
            List<DepartmentDTO> companies = misService.getDepartments();
            List<ServiceMisDTO> services = misService.getServices();
            List<BookingMisDTO> bookings = misService.getPatientBookings(misPatientId);
            List<DocumentMisDTO> documents = misService.getPatientDocuments(misPatientId);

            return MisOrderTemplateData.builder()
                    .patientInfo(patientInfo.orElse(null))
                    .company(companies.isEmpty() ? null : companies.get(0))
                    .doctorName(resolveUserName(misService, "doctor"))
                    .technicianName(resolveUserName(misService, "technician"))
                    .services(services)
                    .bookings(bookings)
                    .documents(documents)
                    .build();
        } catch (Exception e) {
            log.warn("MIS template data load failed, falling back to local data: {}", e.getMessage());
            return empty();
        }
    }

    private String resolveUserName(MisService misService, String login) {
        try {
            List<UserMisDTO> users = misService.getDepartmentUsers(0L);
            if (users == null || users.isEmpty()) {
                return null;
            }
            return users.stream()
                    .filter(u -> login.equalsIgnoreCase(u.getLogin()))
                    .map(UserMisDTO::getFullName)
                    .findFirst()
                    .orElse(users.get(0).getFullName());
        } catch (Exception e) {
            log.debug("User resolution failed: {}", e.getMessage());
            return null;
        }
    }

    private Long parsePatientId(String patientId) {
        if (patientId == null || patientId.isBlank() || !patientId.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Long.parseLong(patientId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private MisOrderTemplateData empty() {
        return MisOrderTemplateData.builder().build();
    }
}
