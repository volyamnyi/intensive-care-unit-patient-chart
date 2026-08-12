package com.superhumans.mis;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import tools.jackson.databind.JsonNode;
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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WireMockMisServiceImpl implements MisService {

    private final MisApiClient misApiClient;
    private final AuditService auditService;

    @lombok.experimental.NonFinal
    private String errorMode = "none";
    @lombok.experimental.NonFinal
    private boolean simulateErrors = false;

    @Override
    public void setErrorMode(String mode) {
        this.errorMode = mode;
        this.simulateErrors = !"none".equals(mode);
    }

    private void checkErrors() {
        if (simulateErrors) {
            switch (errorMode) {
                case "timeout":
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    throw new RuntimeException("MIS timeout");
                case "not_found":
                    throw new RuntimeException("Resource not found in MIS");
                case "unavailable":
                    throw new RuntimeException("MIS service unavailable");
            }
        }
    }

    @Override
    public Optional<PatientDTO> getPatient(Long patientId) {
        checkErrors();
        JsonNode response = misApiClient.callMethod(
                "spzIBPatientSearch",
                new MisApiClient.Param("PatientID", String.valueOf(patientId))
        );
        auditService.logAction("MIS", null, "GET_PATIENT", getUserId());
        return parsePatientList(response).stream()
                .filter(patient -> patient.getId() != null && patient.getId().equals(patientId))
                .findFirst();
    }

    @Override
    public Optional<HospitalizationDTO> getHospitalization(UUID hospitalizationId) {
        checkErrors();
        try {
            // Hospitalization UUIDs encode the MIS patient id in their last 12 decimal digits
            // (same convention as MockMisServiceImpl); resolve it so the request targets the
            // actual patient instead of a literal "0".
            String patientId = hospitalizationId != null && hospitalizationId.toString().length() >= 36
                    ? String.valueOf(Long.parseLong(hospitalizationId.toString().substring(24)))
                    : "0";
            JsonNode response = misApiClient.callMethod(
                    "spzIBPatientScheduleList",
                    new MisApiClient.Param("PatientID", patientId)
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
        checkErrors();
        JsonNode response = misApiClient.callMethod(
                "spzIBUserDetails",
                new MisApiClient.Param("UserLogin", "user" + userId)
        );
        auditService.logAction("MIS", null, "GET_USER", getUserId());
        return parseUserList(response).stream()
                .filter(user -> user.getId() != null && user.getId().equals(userId))
                .findFirst();
    }

    @Override
    public List<UserMisDTO> getDepartmentUsers(Long departmentId) {
        checkErrors();
        JsonNode response = misApiClient.callMethod("spzIBUserDetails");
        auditService.logAction("MIS", null, "GET_DEPARTMENT_USERS", getUserId());
        return parseUserList(response);
    }

    @Override
    public List<DepartmentDTO> getDepartments() {
        checkErrors();
        JsonNode response = misApiClient.callMethod("spzIBCompanyDetails");
        auditService.logAction("MIS", null, "GET_DEPARTMENTS", getUserId());
        return parseDepartmentList(response);
    }

    @Override
    public List<PatientDTO> searchPatients(String query) {
        checkErrors();
        JsonNode response = misApiClient.callMethod("spzIBPatientSearch");
        auditService.logAction("MIS", null, "SEARCH_PATIENTS", getUserId());
        List<PatientDTO> patients = parsePatientList(response);
        if (query == null || query.isBlank()) {
            return patients;
        }
        String lower = query.toLowerCase();
        return patients.stream()
                .filter(p -> containsIgnoreCase(p.getFullName(), lower)
                        || containsIgnoreCase(p.getExternalId1(), lower)
                        || containsIgnoreCase(p.getPhone(), lower))
                .collect(Collectors.toList());
    }

    private static boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }

    @Override
    public List<DictionaryItemDTO> getDictionary(String dictionaryName) {
        checkErrors();
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

    @Override
    public List<ServiceMisDTO> getServices() {
        checkErrors();
        JsonNode response = misApiClient.callMethod("spzIBServiceList");
        auditService.logAction("MIS", null, "GET_SERVICES", getUserId());
        return parseServiceList(response);
    }

    @Override
    public List<BookingMisDTO> getPatientBookings(Long patientId) {
        checkErrors();
        if (patientId == null) {
            return List.of();
        }
        JsonNode response = misApiClient.callMethod(
                "spzIBBookingList",
                new MisApiClient.Param("PatientID", String.valueOf(patientId))
        );
        auditService.logAction("MIS", null, "GET_PATIENT_BOOKINGS", getUserId());
        return parseBookingList(response);
    }

    @Override
    public List<DocumentMisDTO> getPatientDocuments(Long patientId) {
        checkErrors();
        if (patientId == null) {
            return List.of();
        }
        JsonNode response = misApiClient.callMethod(
                "spzIBDocumentList",
                new MisApiClient.Param("PatientID", String.valueOf(patientId))
        );
        auditService.logAction("MIS", null, "GET_PATIENT_DOCUMENTS", getUserId());
        return parseDocumentList(response);
    }

    @Override
    public Optional<PatientInfoMisDTO> getPatientInfo(Long patientId) {
        checkErrors();
        if (patientId == null) {
            return Optional.empty();
        }
        JsonNode response = misApiClient.callMethod(
                "spzIBPatientInfo",
                new MisApiClient.Param("PatientID", String.valueOf(patientId))
        );
        auditService.logAction("MIS", null, "GET_PATIENT_INFO", getUserId());
        return parsePatientInfo(response);
    }

    private List<ServiceMisDTO> parseServiceList(JsonNode response) {
        JsonNode list = response.get("serviceList");
        if (list == null || !list.isArray()) {
            return List.of();
        }
        List<ServiceMisDTO> result = new ArrayList<>();
        for (JsonNode node : list) {
            result.add(ServiceMisDTO.builder()
                    .serviceId(node.has("serviceID") ? node.get("serviceID").asLong() : null)
                    .serviceName(node.has("serviceName") ? node.get("serviceName").asText() : null)
                    .serviceDesc(node.has("serviceDesc") ? node.get("serviceDesc").asText() : null)
                    .serviceCode(node.has("serviceCode") ? node.get("serviceCode").asText() : null)
                    .serviceParentId(node.has("serviceParentID") ? node.get("serviceParentID").asLong() : null)
                    .serviceDuration(node.has("serviceDuration") ? node.get("serviceDuration").asInt() : null)
                    .serviceExternalId(node.has("serviceExternalID") ? node.get("serviceExternalID").asText() : null)
                    .servicePrice(node.has("servicePrice") && !node.get("servicePrice").isNull()
                            ? node.get("servicePrice").asDouble() : null)
                    .build());
        }
        return result;
    }

    private List<BookingMisDTO> parseBookingList(JsonNode response) {
        JsonNode list = response.get("bookingList");
        if (list == null || !list.isArray()) {
            return List.of();
        }
        List<BookingMisDTO> result = new ArrayList<>();
        for (JsonNode node : list) {
            result.add(BookingMisDTO.builder()
                    .bookingId(node.has("bookingID") ? node.get("bookingID").asLong() : null)
                    .bookingName(node.has("bookingName") ? node.get("bookingName").asText() : null)
                    .bookingDate(parseDateTime(node, "bookingDate"))
                    .patientId(node.has("patientID") ? node.get("patientID").asLong() : null)
                    .serviceId(node.has("serviceID") ? node.get("serviceID").asLong() : null)
                    .serviceCode(node.has("serviceCode") ? node.get("serviceCode").asText() : null)
                    .bookingServicePriceValue(node.has("bookingServicePriceValue")
                            && !node.get("bookingServicePriceValue").isNull()
                            ? node.get("bookingServicePriceValue").asDouble() : null)
                    .bookingQuantity(node.has("bookingQuantity") ? node.get("bookingQuantity").asInt() : null)
                    .bookingStatusCode(node.has("bookingStatusCode") ? node.get("bookingStatusCode").asText() : null)
                    .bookingPaymentStatusCode(node.has("bookingPaymentStatusCode")
                            ? node.get("bookingPaymentStatusCode").asText() : null)
                    .bookingCreationDate(parseDateTime(node, "bookingCreationDate"))
                    .bookingExecutionUserLogin(node.has("bookingExecutionUserLogin")
                            ? node.get("bookingExecutionUserLogin").asText() : null)
                    .build());
        }
        return result;
    }

    private List<DocumentMisDTO> parseDocumentList(JsonNode response) {
        JsonNode list = response.get("documentList");
        if (list == null || !list.isArray()) {
            return List.of();
        }
        List<DocumentMisDTO> result = new ArrayList<>();
        for (JsonNode node : list) {
            result.add(DocumentMisDTO.builder()
                    .documentId(node.has("documentID") ? node.get("documentID").asLong() : null)
                    .documentName(node.has("documentName") ? node.get("documentName").asText() : null)
                    .documentCreationDate(parseDateTime(node, "documentCreationDate"))
                    .documentUserLogin(node.has("documentUserLogin") ? node.get("documentUserLogin").asText() : null)
                    .documentTemplateId(node.has("documentTemplateID") ? node.get("documentTemplateID").asLong() : null)
                    .documentTemplateName(node.has("documentTemplateName") ? node.get("documentTemplateName").asText() : null)
                    .documentKindCode(node.has("documentKindCode") ? node.get("documentKindCode").asText() : null)
                    .documentKindName(node.has("documentKindName") ? node.get("documentKindName").asText() : null)
                    .documentApproveStatusCode(node.has("documentApproveStatusCode")
                            ? node.get("documentApproveStatusCode").asText() : null)
                    .documentApproveStatusName(node.has("documentApproveStatusName")
                            ? node.get("documentApproveStatusName").asText() : null)
                    .documentExternalId(node.has("documentExternalID") ? node.get("documentExternalID").asText() : null)
                    .build());
        }
        return result;
    }

    private Optional<PatientInfoMisDTO> parsePatientInfo(JsonNode response) {
        JsonNode info = response.get("patientInfo");
        if (info == null || !info.isObject()) {
            return Optional.empty();
        }
        PatientInfoMisDTO.PatientInfoMisDTOBuilder builder = PatientInfoMisDTO.builder()
                .patientId(info.has("patientID") ? info.get("patientID").asLong() : null)
                .patientName(info.has("patientName") ? info.get("patientName").asText() : null)
                .patientBirthDate(parseDate(info, "patientBirthDate"))
                .patientAddress(info.has("patientAddress") ? info.get("patientAddress").asText() : null)
                .patientPhone(info.has("patientPhone") ? info.get("patientPhone").asText() : null)
                .patientEmail(info.has("patientEmail") ? info.get("patientEmail").asText() : null)
                .patientSexCode(info.has("patientSexCode") ? info.get("patientSexCode").asText() : null)
                .accountValue(info.has("accountValue") && !info.get("accountValue").isNull()
                        ? info.get("accountValue").asDouble() : null)
                .patientBookingSum(info.has("patientBookingSum") && !info.get("patientBookingSum").isNull()
                        ? info.get("patientBookingSum").asDouble() : null)
                .patientDebtSum(info.has("patientDebtSum") && !info.get("patientDebtSum").isNull()
                        ? info.get("patientDebtSum").asDouble() : null);
        if (info.has("patientBookingAct") && info.get("patientBookingAct").isArray()) {
            builder.patientBookingAct(parseBookingListFromAct(info.get("patientBookingAct")));
        }
        return Optional.of(builder.build());
    }

    private List<BookingMisDTO> parseBookingListFromAct(JsonNode act) {
        List<BookingMisDTO> result = new ArrayList<>();
        for (JsonNode node : act) {
            result.add(BookingMisDTO.builder()
                    .bookingId(node.has("bookingID") ? node.get("bookingID").asLong() : null)
                    .bookingName(node.has("bookingName") ? node.get("bookingName").asText() : null)
                    .bookingDate(parseDateTime(node, "bookingDate"))
                    .serviceId(node.has("serviceID") ? node.get("serviceID").asLong() : null)
                    .serviceCode(node.has("serviceCode") ? node.get("serviceCode").asText() : null)
                    .bookingServicePriceValue(node.has("bookingServicePriceValue")
                            && !node.get("bookingServicePriceValue").isNull()
                            ? node.get("bookingServicePriceValue").asDouble() : null)
                    .bookingQuantity(node.has("bookingQuantity") ? node.get("bookingQuantity").asInt() : null)
                    .build());
        }
        return result;
    }

    private LocalDate parseDate(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull() || node.get(field).asText().isBlank()) {
            return null;
        }
        String raw = node.get(field).asText();
        return LocalDate.parse(raw.substring(0, 10));
    }

    private LocalDateTime parseDateTime(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull() || node.get(field).asText().isBlank()) {
            return null;
        }
        String raw = node.get(field).asText();
        String normalized = raw.replace(' ', 'T');
        if (normalized.length() >= 19) {
            normalized = normalized.substring(0, 19);
        }
        return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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
                    .departmentId(node.has("patientDepartmentID") ? node.get("patientDepartmentID").asLong() : null)
                    .build();
            result.add(patient);
        }
        return result;
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
                    .address(node.has("companyAddress") ? node.get("companyAddress").asText() : null)
                    .email(node.has("companyEmail") ? node.get("companyEmail").asText() : null)
                    .phone(node.has("companyPhone") ? node.get("companyPhone").asText() : null)
                    .externalId1(node.has("companyExternalID1") ? node.get("companyExternalID1").asText() : null)
                    .externalId2(node.has("companyExternalID2") ? node.get("companyExternalID2").asText() : null)
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
