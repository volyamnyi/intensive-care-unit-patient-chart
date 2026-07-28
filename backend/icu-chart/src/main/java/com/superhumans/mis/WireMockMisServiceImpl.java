package com.superhumans.mis;

import com.fasterxml.jackson.databind.JsonNode;
import com.superhumans.mis.dto.*;
import com.superhumans.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mis.wiremock-enabled", havingValue = "true", matchIfMissing = true)
public class WireMockMisServiceImpl implements MisService {

    private final MisApiClient misApiClient;
    private final AuditService auditService;

    @Override
    public Optional<PatientDTO> getPatient(Long patientId) {
        JsonNode response = misApiClient.callMethod(
                "spzIBPatientSearch",
                new MisApiClient.Param("PatientID", String.valueOf(patientId))
        );
        auditService.logAction("MIS", null, "GET_PATIENT", getUserId());
        return parsePatient(response);
    }

    @Override
    public Optional<HospitalizationDTO> getHospitalization(UUID hospitalizationId) {
        try {
            JsonNode response = misApiClient.callMethod(
                    "spzIBPatientScheduleList",
                    new MisApiClient.Param("PatientID", "0")
            );
            auditService.logAction("MIS", null, "GET_HOSPITALIZATION", getUserId());

            JsonNode scheduleList = response.get("scheduleList");
            if (scheduleList != null && scheduleList.isArray() && scheduleList.size() > 0) {
                JsonNode first = scheduleList.get(0);
                return Optional.of(HospitalizationDTO.builder()
                        .id(hospitalizationId)
                        .patientId(first.has("patientID") ? first.get("patientID").asLong() : null)
                        .departmentId(1L)
                        .admissionDate(LocalDateTime.now().minusDays(3))
                        .diagnosis("Діагноз при госпіталізації")
                        .departmentName("Відділення анестезіології та інтенсивної терапії")
                        .room("101")
                        .bed("A")
                        .build());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Hospitalization lookup via MIS API failed, using fallback: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserMisDTO> getUser(Long userId) {
        JsonNode response = misApiClient.callMethod(
                "spzIBUserDetails",
                new MisApiClient.Param("UserLogin", "user" + userId)
        );
        auditService.logAction("MIS", null, "GET_USER", getUserId());
        return parseUser(response);
    }

    @Override
    public List<UserMisDTO> getDepartmentUsers(Long departmentId) {
        JsonNode response = misApiClient.callMethod("spzIBUserDetails");
        auditService.logAction("MIS", null, "GET_DEPARTMENT_USERS", getUserId());
        return parseUserList(response);
    }

    @Override
    public List<DepartmentDTO> getDepartments() {
        JsonNode response = misApiClient.callMethod("spzIBCompanyDetails");
        auditService.logAction("MIS", null, "GET_DEPARTMENTS", getUserId());
        return parseDepartmentList(response);
    }

    @Override
    public List<PatientDTO> searchPatients(String query) {
        if (query == null || query.isBlank()) {
            JsonNode response = misApiClient.callMethod("spzIBPatientSearch");
            auditService.logAction("MIS", null, "SEARCH_PATIENTS", getUserId());
            return parsePatientList(response);
        }
        JsonNode response = misApiClient.callMethod(
                "spzIBPatientSearch",
                new MisApiClient.Param("PatientName", query)
        );
        auditService.logAction("MIS", null, "SEARCH_PATIENTS", getUserId());
        return parsePatientList(response);
    }

    @Override
    public List<DictionaryItemDTO> getDictionary(String dictionaryName) {
        return switch (dictionaryName) {
            case "orderCategories" -> List.of(
                    new DictionaryItemDTO("MEDICATION", "Медикаменти"),
                    new DictionaryItemDTO("INFUSION", "Інфузії"),
                    new DictionaryItemDTO("LAB", "Аналізи"),
                    new DictionaryItemDTO("PROCEDURE", "Маніпуляції"),
                    new DictionaryItemDTO("VENTILATION", "ШВЛ"),
                    new DictionaryItemDTO("NUTRITION", "Харчування"),
                    new DictionaryItemDTO("OTHER", "Інші"));
            case "noteTypes" -> List.of(
                    new DictionaryItemDTO("DOCTOR_NOTE", "Лікарський запис"),
                    new DictionaryItemDTO("NURSE_NOTE", "Сестринський запис"),
                    new DictionaryItemDTO("SHIFT_REPORT", "Звіт за зміну"));
            case "consciousness" -> List.of(
                    new DictionaryItemDTO("CLEAR", "Ясна"),
                    new DictionaryItemDTO("STUPOR", "Ступор"),
                    new DictionaryItemDTO("SOPOR", "Сопор"),
                    new DictionaryItemDTO("COMA", "Кома"),
                    new DictionaryItemDTO("SEDATED", "Седація"));
            case "bookingStatus" -> {
                JsonNode response = misApiClient.callMethod("spzIBBookingStatusDictionary");
                auditService.logAction("MIS", null, "GET_DICTIONARY", getUserId());
                yield parseDictionaryList(response, "bookingStatusList",
                        "bookingStatusCode", "bookingStatusName");
            }
            case "paymentStatus" -> {
                JsonNode response = misApiClient.callMethod("spzIBBookingPaymentStatusDictionary");
                auditService.logAction("MIS", null, "GET_DICTIONARY", getUserId());
                yield parseDictionaryList(response, "bookingPaymentStatusList",
                        "bookingPaymentStatusCode", "bookingPaymentStatusName");
            }
            case "scheduleStatus" -> {
                JsonNode response = misApiClient.callMethod("spzIBScheduleStatusDictionary");
                auditService.logAction("MIS", null, "GET_DICTIONARY", getUserId());
                yield parseDictionaryList(response, "scheduleStatusList",
                        "scheduleStatusCode", "scheduleStatusName");
            }
            default -> {
                log.warn("Unknown dictionary: {}", dictionaryName);
                yield List.of();
            }
        };
    }

    @Override
    public boolean sendPdf(UUID clinicalDayId, byte[] pdfContent, String fileName, int version) {
        auditService.logAction("MIS", clinicalDayId, "SEND_PDF", getUserId());
        log.info("PDF sent to MIS: clinicalDayId={}, fileName={}, version={}", clinicalDayId, fileName, version);
        return true;
    }

    @Override
    public List<MedicineMisDTO> searchMedicineCatalog(String keyword) {
        log.info("MEDICINE_CATALOG search via MIS not available with WireMock. Use MockMIS mode.");
        auditService.logAction("MIS", null, "SEARCH_MEDICINE_CATALOG", getUserId());
        return List.of();
    }

    @Override
    public List<AllergyMisDTO> getPatientAllergies(Long patientId) {
        log.info("ALLERGY lookup via MIS not available with WireMock. Use MockMIS mode.");
        auditService.logAction("MIS", null, "GET_ALLERGIES", getUserId());
        return List.of();
    }

    private Optional<PatientDTO> parsePatient(JsonNode response) {
        List<PatientDTO> patients = parsePatientList(response);
        return patients.isEmpty() ? Optional.empty() : Optional.of(patients.get(0));
    }

    private List<PatientDTO> parsePatientList(JsonNode response) {
        JsonNode patientList = response.get("patientList");
        if (patientList == null || !patientList.isArray()) {
            return List.of();
        }
        List<PatientDTO> result = new ArrayList<>();
        for (JsonNode node : patientList) {
            PatientDTO patient = PatientDTO.builder()
                    .id(node.has("patientID") ? node.get("patientID").asLong() : null)
                    .fullName(node.has("patientName") ? node.get("patientName").asText() : null)
                    .birthDate(node.has("patientBirthDate") ?
                            LocalDate.parse(node.get("patientBirthDate").asText().substring(0, 10)) : null)
                    .sexCode(node.has("patientSexCode") ? node.get("patientSexCode").asText() : null)
                    .address(node.has("patientAddress") ? node.get("patientAddress").asText() : null)
                    .phone(node.has("patientPhone") ? node.get("patientPhone").asText() : null)
                    .email(node.has("patientEmail") ? node.get("patientEmail").asText() : null)
                    .externalId1(node.has("patientExternalID1") ? node.get("patientExternalID1").asText() : null)
                    .externalId2(node.has("patientExternalID2") ? node.get("patientExternalID2").asText() : null)
                    .room(node.has("patientRoomNumber") ? node.get("patientRoomNumber").asText() : null)
                    .bed(node.has("patientBedNumber") ? node.get("patientBedNumber").asText() : null)
                    .doctorName(node.has("patientDoctor") ? node.get("patientDoctor").asText() : null)
                    .build();
            result.add(patient);
        }
        return result;
    }

    private Optional<UserMisDTO> parseUser(JsonNode response) {
        List<UserMisDTO> users = parseUserList(response);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    private List<UserMisDTO> parseUserList(JsonNode response) {
        JsonNode userList = response.get("userList");
        if (userList == null || !userList.isArray()) {
            return List.of();
        }
        List<UserMisDTO> result = new ArrayList<>();
        for (JsonNode node : userList) {
            UserMisDTO user = UserMisDTO.builder()
                    .id(node.has("userID") ? node.get("userID").asLong() : null)
                    .login(node.has("userLogin") ? node.get("userLogin").asText() : null)
                    .fullName(node.has("userName") ? node.get("userName").asText() : null)
                    .shortName(node.has("userShortName") ? node.get("userShortName").asText() : null)
                    .specialityCode(node.has("userSpecialityCode") ? node.get("userSpecialityCode").asText() : null)
                    .specialityName(node.has("userSpecialityName") ? node.get("userSpecialityName").asText() : null)
                    .email(node.has("userEmail") ? node.get("userEmail").asText() : null)
                    .phone(node.has("userPhone") ? node.get("userPhone").asText() : null)
                    .build();
            result.add(user);
        }
        return result;
    }

    private List<DepartmentDTO> parseDepartmentList(JsonNode response) {
        JsonNode companyList = response.get("companyList");
        if (companyList == null || !companyList.isArray()) {
            return List.of();
        }
        List<DepartmentDTO> result = new ArrayList<>();
        for (JsonNode node : companyList) {
            DepartmentDTO dept = DepartmentDTO.builder()
                    .id(node.has("companyGUID") ? (long) node.get("companyGUID").asText().hashCode() : null)
                    .name(node.has("companyName") ? node.get("companyName").asText() : null)
                    .code(node.has("companyShortName") ? node.get("companyShortName").asText() : null)
                    .build();
            result.add(dept);
        }
        return result;
    }

    private List<DictionaryItemDTO> parseDictionaryList(JsonNode response, String listField,
                                                         String codeField, String nameField) {
        JsonNode list = response.get(listField);
        if (list == null || !list.isArray()) {
            return List.of();
        }
        List<DictionaryItemDTO> result = new ArrayList<>();
        for (JsonNode node : list) {
            result.add(new DictionaryItemDTO(
                    node.has(codeField) ? node.get(codeField).asText() : null,
                    node.has(nameField) ? node.get(nameField).asText() : null
            ));
        }
        return result;
    }

    private Long getUserId() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null && auth.getCredentials() instanceof Long id) {
                return id;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
