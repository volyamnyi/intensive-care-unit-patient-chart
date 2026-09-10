package com.superhumans.mis;

import com.superhumans.mis.dto.*;
import com.superhumans.service.AuditService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Real MIS {@link MisService} implementation.
 * <p>
 * Talks to the live MIS API through {@link MisApiClient} (stored-procedure
 * envelope {@code {name, params, installationId}}, Bearer-authenticated). The
 * ICU Chart is a <b>READ-ONLY</b> client: only Search/Details retrieval is
 * used; no MIS
 * write method is ever invoked.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MisServiceImpl implements MisService {

    private final MisApiClient misApiClient;
    private final AuditService auditService;

    static final String SPI_PATIENT_PROCEDURE = "spiPatientProsthesCheck";
    static final String SPI_DOCUMENT_PROCEDURE = "spiDocumentProsthesCheck";
    static final String SPI_MEDICINE_PROCEDURE = "spiMedicineItemKindDetails";

    @Override
    public Optional<PatientDTO> getPatient(Long patientId) {
        return getAllPatientsUnderTreatment().stream()
                .filter(patient -> patient.getId() != null && patient.getId().equals(patientId))
                .findFirst();
    }

    @Override
    public List<PatientDTO> searchPatients(String query) {
        return filterPatients(getAllPatientsUnderTreatment(), query);
    }

    @Override
    public List<PatientDTO> getAllPatientsUnderTreatment() {
        JsonNode response = misApiClient.callMethod(SPI_PATIENT_PROCEDURE);
        auditService.logAction("MIS", null, "GET_ALL_PATIENTS", getUserId());
        return parsePatientList(response);
    }

    private static List<PatientDTO> filterPatients(List<PatientDTO> patients, String query) {
        if (query == null || query.isBlank()) {
            return patients;
        }
        String lower = query.toLowerCase();
        return patients.stream()
                .filter(p -> (p.getId() != null && String.valueOf(p.getId()).contains(lower))
                        || containsIgnoreCase(p.getFullName(), lower)
                        || containsIgnoreCase(p.getExternalId1(), lower)
                        || containsIgnoreCase(p.getPhone(), lower))
                .collect(Collectors.toList());
    }

    private static boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }

    @Override
    public List<MedicineMisDTO> searchMedicineCatalog(String keyword) {
        JsonNode response = misApiClient.callMethod(SPI_MEDICINE_PROCEDURE);
        auditService.logAction("MIS", null, "SEARCH_MEDICINE_CATALOG", getUserId());
        List<MedicineMisDTO> catalog = parseMedicineList(response);
        if (keyword == null || keyword.isBlank()) {
            return catalog;
        }
        String lower = keyword.toLowerCase();
        return catalog.stream()
                .filter(m -> m.getName() != null
                        && m.getName().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentMisDTO> getPatientDocuments(Long patientId) {
        if (patientId == null) {
            return List.of();
        }
        JsonNode response = misApiClient.callMethod(
                SPI_DOCUMENT_PROCEDURE,
                new MisApiClient.Param("PatientID", String.valueOf(patientId))
        );
        auditService.logAction("MIS", null, "GET_PATIENT_DOCUMENTS", getUserId());
        return parseDocumentList(response);
    }

    private List<DocumentMisDTO> parseDocumentList(JsonNode response) {
        JsonNode list = firstNonEmptyArray(
                response, SPI_DOCUMENT_PROCEDURE, "documentList", "documents");
        if (list == null) {
            return List.of();
        }
        List<DocumentMisDTO> result = new ArrayList<>();
        for (JsonNode node : list) {
            result.add(DocumentMisDTO.builder()
                    .documentId(longOrNull(node, "documentId", "documentID"))
                    .documentName(textOrNull(node, "documentName", "documentTemplateName"))
                    .documentCreationDate(parseFlexibleDateTime(node, "documentCreationDate"))
                    .documentUserLogin(textOrNull(node, "documentUserLogin"))
                    .documentTemplateId(longOrNull(node, "documentTemplateId", "documentTemplateID"))
                    .documentTemplateName(textOrNull(node, "documentTemplateName"))
                    .documentKindCode(textOrNull(node, "documentKindCode"))
                    .documentKindName(textOrNull(node, "documentKindName"))
                    .documentApproveStatusCode(textOrNull(node, "documentApproveStatusCode"))
                    .documentApproveStatusName(textOrNull(node, "documentApproveStatusName"))
                    .documentExternalId(textOrNull(node, "documentExternalID", "documentExternalId"))
                    .documentUrl(textOrNull(node, "documentUrl", "documentURL"))
                    .patientId(longOrNull(node, "patientID", "patientId"))
                    .orderDate(parseFlexibleDateTime(node, "orderDate"))
                    .patientFullName(textOrNull(node, "patientFullName", "patientName"))
                    .patientAddress(textOrNull(node, "patientAddress"))
                    .productCode(textOrNull(node, "productCode"))
                    .productName(textOrNull(node, "productName"))
                    .mobilityLevel(textOrNull(node, "mobilityLevel", "mobilityLevelCode"))
                    .patientGender(textOrNull(node, "patientGender", "patientSexCode"))
                    .age(intOrNull(node, "age", "patientAge"))
                    .height(intOrNull(node, "height", "patientHeight"))
                    .weight(intOrNull(node, "weight", "patientWeight"))
                    .note(textOrNull(node, "note"))
                    .build());
        }
        return result;
    }

    private List<MedicineMisDTO> parseMedicineList(JsonNode response) {
        JsonNode list = firstNonEmptyArray(response, "medicineItemKindDetails", "medicineList",
                "medicines", "items");
        if (list == null) {
            return List.of();
        }
        List<MedicineMisDTO> result = new ArrayList<>();
        for (JsonNode node : list) {
            result.add(MedicineMisDTO.builder()
                    .id(longOrNull(node, "itemKindID", "itemKindId", "medicineID", "medicineId"))
                    .name(textOrNull(node, "itemKindName", "medicineName"))
                    .categoryRef(intOrNull(node, "medicineCategoryRef"))
                    .ptgCode(textOrNull(node, "medicinePtgCode"))
                    .itemKindCode(textOrNull(node, "itemKindCode"))
                    .itemKindAtc(textOrNull(node, "itemKindATC", "itemKindAtc"))
                    .itemKindUnit(textOrNull(node, "itemKindUnit"))
                    .itemKindManufacturer(textOrNull(node, "itemKindManufacturer"))
                    .itemKindIsDisabled(booleanOrNull(node, "itemKindIsDisabled"))
                    .itemKindEan(textOrNull(node, "itemKindEAN", "itemKindEan"))
                    .itemKindIsDivisible(booleanOrNull(node, "itemKindIsDivisible"))
                    .itemKindDlc(textOrNull(node, "itemKindDLC", "itemKindDlc"))
                    .medicineCategoryId(longOrNull(node, "medicineCategoryID", "medicineCategoryId"))
                    .medicineCategoryName(textOrNull(node, "medicineCategoryName"))
                    .medicinePackageId(longOrNull(node, "medicinePackageID", "medicinePackageId"))
                    .medicinePackageName(textOrNull(node, "medicinePackageName"))
                    .build());
        }
        return result;
    }

    private static Boolean booleanOrNull(JsonNode node, String... fields) {
        for (String field : fields) {
            if (!node.has(field) || node.get(field).isNull()) {
                continue;
            }
            JsonNode value = node.get(field);
            if (value.isBoolean()) {
                return value.asBoolean();
            }
            if (value.isNumber()) {
                return value.asInt() != 0;
            }
            String text = value.asText();
            if (text != null && !text.isBlank()) {
                String normalized = text.trim().toLowerCase();
                if (normalized.equals("true") || normalized.equals("1")
                        || normalized.equals("y") || normalized.equals("yes")) {
                    return true;
                }
                if (normalized.equals("false") || normalized.equals("0")
                        || normalized.equals("n") || normalized.equals("no")) {
                    return false;
                }
            }
        }
        return null;
    }

    private List<PatientDTO> parsePatientList(JsonNode response) {
        JsonNode patientList = firstNonEmptyArray(
                response, SPI_PATIENT_PROCEDURE, "patientList", "patients");
        if (patientList == null) {
            return List.of();
        }
        List<PatientDTO> result = new ArrayList<>();
        for (JsonNode node : patientList) {
            PatientDTO patient = PatientDTO.builder()
                    .id(longOrNull(node, "id", "patientID", "patientId"))
                    .fullName(textOrNull(node, "fullName", "patientName", "patientFullName"))
                    .birthDate(parseDateOrNull(node, "birthDate", "patientBirthDate"))
                    .sexCode(textOrNull(node, "sexCode", "patientSexCode"))
                    .address(textOrNull(node, "address", "patientAddress"))
                    .phone(textOrNull(node, "phone", "patientPhone"))
                    .email(textOrNull(node, "email", "patientEmail"))
                    .externalId1(textOrNull(node, "patientExternalID1", "patientExternalId1"))
                    .externalId2(textOrNull(node, "patientExternalID2", "patientExternalId2"))
                    .height(intOrNull(node, "height", "patientHeight"))
                    .weight(intOrNull(node, "weight", "patientWeight"))
                    .bloodGroup(textOrNull(node, "bloodGroup", "patientBloodGroup"))
                    .rhFactor(textOrNull(node, "rhFactor", "patientRhFactor"))
                    .room(textOrNull(node, "room", "patientRoomNumber", "roomNumber"))
                    .bed(textOrNull(node, "bed", "patientBedNumber", "bedNumber"))
                    .doctorName(textOrNull(node, "doctorName", "patientDoctor"))
                    .departmentId(longOrNull(node, "departmentId", "patientDepartmentID",
                            "patientDepartmentId", "departmentID"))
                    .build();
            result.add(patient);
        }
        return result;
    }

    /**
     * Finds the first non-empty array under any of the given wrapper keys —
     * the real API wraps results in the procedure name (e.g.
     * {@code spiPatientProsthesCheck}), while older mock shapes used generic
     * keys.
     */
    private static JsonNode firstNonEmptyArray(JsonNode response, String... wrapperKeys) {
        if (response == null) {
            return null;
        }
        for (String key : wrapperKeys) {
            JsonNode node = response.get(key);
            if (node != null && node.isArray() && node.size() > 0) {
                return node;
            }
        }
        return null;
    }

    private LocalDate parseDateOrNull(JsonNode node, String... fields) {
        for (String field : fields) {
            if (!node.has(field) || node.get(field).isNull()) {
                continue;
            }
            String raw = node.get(field).asText().trim();
            if (raw.length() >= 10) {
                try {
                    return LocalDate.parse(raw.substring(0, 10));
                } catch (Exception ignored) {
                    continue;
                }
            }
        }
        return null;
    }

    private static Integer intOrNull(JsonNode node, String field) {
        return intOrNull(node, new String[]{field});
    }

    private static Integer intOrNull(JsonNode node, String... fields) {
        for (String field : fields) {
            if (!node.has(field) || node.get(field).isNull()) {
                continue;
            }
            try {
                return node.get(field).asInt();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static Long longOrNull(JsonNode node, String... fields) {
        for (String field : fields) {
            if (!node.has(field) || node.get(field).isNull()) {
                continue;
            }
            try {
                return node.get(field).asLong();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static String textOrNull(JsonNode node, String... fields) {
        for (String field : fields) {
            if (!node.has(field) || node.get(field).isNull()) {
                continue;
            }
            String text = node.get(field).asText();
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private LocalDateTime parseFlexibleDateTime(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()
                || node.get(field).asText().isBlank()) {
            return null;
        }
        String raw = node.get(field).asText().trim().replace(' ', 'T');
        try {
            if (raw.length() >= 19) {
                return LocalDateTime.parse(raw.substring(0, 19),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return LocalDate.parse(raw.substring(0, 10)).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
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
