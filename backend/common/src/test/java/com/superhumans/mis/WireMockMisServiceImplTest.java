package com.superhumans.mis;

import com.superhumans.mis.dto.*;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for {@link WireMockMisServiceImpl} (issue #191). The client is
 * mocked; responses are real Jackson trees built from inline JSON mirroring
 * the wiremock fixtures. Every public method of the service is exercised.
 */
@ExtendWith(MockitoExtension.class)
class WireMockMisServiceImplTest {

    @Mock
    MisApiClient misApiClient;

    @Mock
    AuditService auditService;

    WireMockMisServiceImpl service;

    final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new WireMockMisServiceImpl(misApiClient, auditService);
    }

    private JsonNode json(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final String PATIENTS_JSON = """
            {"patientList":[
              {"patientID":1001,"patientName":"Петренко Іван Сергійович",
               "patientBirthDate":"1978-03-15","patientSexCode":"MAL",
               "patientAddress":"м. Київ","patientPhone":"380501234567",
               "patientEmail":"ivan@mail.com","patientExternalID1":"МК-001234",
               "patientExternalID2":"301020251234","patientRoomNumber":"12",
               "patientBedNumber":"3","patientDoctor":"Докторов",
               "patientDepartmentID":1},
              {"patientID":900001,"patientName":"Сніжко Іван Петрович",
               "patientBirthDate":"1991-03-14","patientSexCode":"MAL"}
            ]}
            """;

    private static final String USERS_JSON = """
            {"userList":[
              {"userID":11,"userLogin":"doctor1","userName":"Олександр Мельник",
               "userShortName":"О.Мельник","userSpecialityCode":"101",
               "userSpecialityName":"Лікар-анестезіолог","userEmail":"m@h.ua",
               "userPhone":"380501111111"},
              {"userID":13,"userLogin":"nurse1","userName":"Олена Ткаченко",
               "userShortName":"О.Ткаченко","userSpecialityCode":"201",
               "userSpecialityName":"Медсестра ВАІТ"}
            ]}
            """;

    private static final String COMPANIES_JSON = """
            {"companyList":[
              {"companyGUID":"guid-aaa","companyName":"Інтернальна медицина",
               "companyShortName":"ІМ","companyAddress":"вул. Хрещатик, 1",
               "companyEmail":"im@hospital.ua","companyPhone":"380441112233",
               "companyExternalID1":"E-IM","companyExternalID2":""}
            ]}
            """;

    // ---- searchPatients ----

    @Test
    void searchPatients_withoutQuery_returnsFullList() {
        when(misApiClient.callMethod("spzIBPatientSearch"))
                .thenReturn(json(PATIENTS_JSON));

        List<PatientDTO> result = service.searchPatients(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1001L);
        assertThat(result.get(0).getFullName()).isEqualTo("Петренко Іван Сергійович");
        assertThat(result.get(0).getRoom()).isEqualTo("12");
        verify(auditService).logAction("MIS", null, "SEARCH_PATIENTS", null);
    }

    @Test
    void searchPatients_withFullNameFilter_filtersCaseInsensitive() {
        when(misApiClient.callMethod("spzIBPatientSearch"))
                .thenReturn(json(PATIENTS_JSON));

        List<PatientDTO> result = service.searchPatients("сніжко");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(900001L);
    }

    @Test
    void searchPatients_byExternalId_matches() {
        when(misApiClient.callMethod("spzIBPatientSearch"))
                .thenReturn(json(PATIENTS_JSON));

        List<PatientDTO> result = service.searchPatients("МК-001234");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalId1()).isEqualTo("МК-001234");
    }

    @Test
    void searchPatients_byPhone_matches() {
        when(misApiClient.callMethod("spzIBPatientSearch"))
                .thenReturn(json(PATIENTS_JSON));

        List<PatientDTO> result = service.searchPatients("380501234567");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPhone()).isEqualTo("380501234567");
    }

    @Test
    void searchPatients_noMatch_returnsEmptyList() {
        when(misApiClient.callMethod("spzIBPatientSearch"))
                .thenReturn(json(PATIENTS_JSON));

        assertThat(service.searchPatients(" nonexistent ")).isEmpty();
    }

    @Test
    void searchPatients_emptyPatientList_returnsEmptyList() {
        when(misApiClient.callMethod("spzIBPatientSearch"))
                .thenReturn(json("{\"patientList\":[]}"));

        assertThat(service.searchPatients(null)).isEmpty();
    }

    @Test
    void searchPatients_missingPatientListField_returnsEmptyList() {
        when(misApiClient.callMethod("spzIBPatientSearch"))
                .thenReturn(json("{}"));

        assertThat(service.searchPatients(null)).isEmpty();
    }

    // ---- getPatient ----

    @Test
    void getPatient_whenFound_returnsPatient() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json(PATIENTS_JSON));

        Optional<PatientDTO> result = service.getPatient(1001L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1001L);
    }

    @Test
    void getPatient_whenNotInList_returnsEmpty() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json(PATIENTS_JSON));

        assertThat(service.getPatient(999999L)).isEmpty();
    }

    // ---- users ----

    @Test
    void getUser_whenFound_parsesAllFields() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json(USERS_JSON));

        Optional<UserMisDTO> result = service.getUser(11L);

        assertThat(result).isPresent();
        UserMisDTO user = result.get();
        assertThat(user.getLogin()).isEqualTo("doctor1");
        assertThat(user.getSpecialityCode()).isEqualTo("101");
        assertThat(user.getEmail()).isEqualTo("m@h.ua");
    }

    @Test
    void getUser_unknownId_returnsEmpty() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json(USERS_JSON));

        assertThat(service.getUser(999L)).isEmpty();
    }

    @Test
    void getDepartmentUsers_parsesUserList() {
        when(misApiClient.callMethod("spzIBUserDetails")).thenReturn(json(USERS_JSON));

        List<UserMisDTO> result = service.getDepartmentUsers(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(1).getLogin()).isEqualTo("nurse1");
        verify(auditService).logAction("MIS", null, "GET_DEPARTMENT_USERS", null);
    }

    // ---- departments ----

    @Test
    void getDepartments_derivesIdFromCompanyGuidHashCode() {
        when(misApiClient.callMethod("spzIBCompanyDetails")).thenReturn(json(COMPANIES_JSON));

        List<DepartmentDTO> result = service.getDepartments();

        assertThat(result).hasSize(1);
        DepartmentDTO dept = result.get(0);
        assertThat(dept.getId()).isEqualTo((long) "guid-aaa".hashCode());
        assertThat(dept.getName()).isEqualTo("Інтернальна медицина");
        assertThat(dept.getCode()).isEqualTo("ІМ");
    }

    @Test
    void getDepartments_missingCompanyList_returnsEmptyList() {
        when(misApiClient.callMethod("spzIBCompanyDetails")).thenReturn(json("{}"));

        assertThat(service.getDepartments()).isEmpty();
    }

    // ---- dictionaries ----

    @Test
    void getDictionary_hardcodedOrderCategories_servedWithoutClientCall() {
        List<DictionaryItemDTO> result = service.getDictionary("orderCategories");

        assertThat(result).hasSize(7);
        assertThat(result.get(0).getCode()).isEqualTo("MEDICATION");
    }

    @Test
    void getDictionary_hardcodedNoteTypes_served() {
        assertThat(service.getDictionary("noteTypes")).hasSize(3);
    }

    @Test
    void getDictionary_hardcodedConsciousness_served() {
        assertThat(service.getDictionary("consciousness")).hasSize(5);
    }

    @Test
    void getDictionary_bookingStatus_parsesFixtureResponse() {
        when(misApiClient.callMethod("spzIBBookingStatusDictionary"))
                .thenReturn(json("""
                        {"bookingStatusList":[
                          {"bookingStatusCode":"CREATED","bookingStatusName":"Створено"},
                          {"bookingStatusCode":"DONE","bookingStatusName":"Виконано"}
                        ]}
                        """));

        List<DictionaryItemDTO> result = service.getDictionary("bookingStatus");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("CREATED");
    }

    @Test
    void getDictionary_paymentStatus_parsesFixtureResponse() {
        when(misApiClient.callMethod("spzIBBookingPaymentStatusDictionary"))
                .thenReturn(json("""
                        {"bookingPaymentStatusList":[
                          {"bookingPaymentStatusCode":"PAID","bookingPaymentStatusName":"Оплачено"}
                        ]}
                        """));

        List<DictionaryItemDTO> result = service.getDictionary("paymentStatus");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("PAID");
    }

    @Test
    void getDictionary_scheduleStatus_parsesFixtureResponse() {
        when(misApiClient.callMethod("spzIBScheduleStatusDictionary"))
                .thenReturn(json("""
                        {"scheduleStatusList":[
                          {"scheduleStatusCode":"PLANNED","scheduleStatusName":"Заплановано"}
                        ]}
                        """));

        List<DictionaryItemDTO> result = service.getDictionary("scheduleStatus");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Заплановано");
    }

    @Test
    void getDictionary_unknownName_returnsEmptyList() {
        assertThat(service.getDictionary("no-such-dictionary")).isEmpty();
    }

    // ---- prosthetics support methods ----

    @Test
    void getServices_parsesServiceList() {
        when(misApiClient.callMethod("spzIBServiceList")).thenReturn(json("""
                {"serviceList":[
                  {"serviceID":500,"serviceName":"Протез передпліччя",
                   "serviceDesc":"Косметичний","serviceCode":"ISO-9999",
                   "serviceParentID":100,"serviceDuration":60,
                   "serviceExternalID":"SE-1","servicePrice":12000.5}
                ]}
                """));

        List<ServiceMisDTO> result = service.getServices();

        assertThat(result).hasSize(1);
        ServiceMisDTO svc = result.get(0);
        assertThat(svc.getServiceId()).isEqualTo(500L);
        assertThat(svc.getServicePrice()).isEqualTo(12000.5);
        assertThat(svc.getServiceCode()).isEqualTo("ISO-9999");
    }

    @Test
    void getServices_missingList_returnsEmptyList() {
        when(misApiClient.callMethod("spzIBServiceList")).thenReturn(json("{}"));

        assertThat(service.getServices()).isEmpty();
    }

    @Test
    void getPatientBookings_parsesDatesAndNullSafeFields() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json("""
                        {"bookingList":[
                          {"bookingID":900,"bookingName":"Прийом",
                           "bookingDate":"2026-08-24 10:30:00","patientID":900001,
                           "serviceID":500,"serviceCode":"ISO-9999",
                           "bookingServicePriceValue":1500.0,"bookingQuantity":1,
                           "bookingStatusCode":"DONE",
                           "bookingCreationDate":"2026-08-01 08:00:00",
                           "bookingExecutionUserLogin":"doctor1"}
                        ]}
                        """));

        List<BookingMisDTO> result = service.getPatientBookings(900001L);

        assertThat(result).hasSize(1);
        BookingMisDTO booking = result.get(0);
        assertThat(booking.getBookingDate().toString()).startsWith("2026-08-24T10:30");
        assertThat(booking.getBookingQuantity()).isEqualTo(1);
    }

    @Test
    void getPatientBookings_nullPatientId_returnsEmptyList() {
        assertThat(service.getPatientBookings(null)).isEmpty();
    }

    @Test
    void getPatientDocuments_parsesDocumentFields() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json("""
                        {"documentList":[
                          {"documentID":700,"documentName":"Направлення.pdf",
                           "documentCreationDate":"2026-07-01 12:00:00",
                           "documentUserLogin":"doctor1","documentTemplateID":55,
                           "documentTemplateName":"Направлення",
                           "documentKindCode":"REFERRAL","documentKindName":"Направлення",
                           "documentApproveStatusCode":"APPROVED",
                           "documentApproveStatusName":"Підтверджено",
                           "documentExternalID":"DOC-EXT-1"}
                        ]}
                        """));

        List<DocumentMisDTO> result = service.getPatientDocuments(900001L);

        assertThat(result).hasSize(1);
        DocumentMisDTO doc = result.get(0);
        assertThat(doc.getDocumentId()).isEqualTo(700L);
        assertThat(doc.getDocumentKindCode()).isEqualTo("REFERRAL");
    }

    @Test
    void getPatientDocuments_nullPatientId_returnsEmptyList() {
        assertThat(service.getPatientDocuments(null)).isEmpty();
    }

    @Test
    void getPatientInfo_parsesAccountBookingsAndDebt() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json("""
                        {"patientInfo":{
                          "patientID":900001,"patientName":"Сніжко Іван Петрович",
                          "patientBirthDate":"1991-03-14","patientAddress":"м. Київ",
                          "patientPhone":"380501112222","patientEmail":"s@i.ua",
                          "patientSexCode":"MAL","accountValue":2500.0,
                          "patientBookingSum":800.0,"patientDebtSum":null,
                          "patientBookingAct":[
                            {"bookingID":910,"bookingName":"Примірка",
                             "bookingDate":"2026-09-01 09:00:00","serviceID":501,
                             "serviceCode":"ISO-9998","bookingServicePriceValue":400.0,
                             "bookingQuantity":2}
                          ]
                        }}
                        """));

        Optional<PatientInfoMisDTO> result = service.getPatientInfo(900001L);

        assertThat(result).isPresent();
        PatientInfoMisDTO info = result.get();
        assertThat(info.getPatientId()).isEqualTo(900001L);
        assertThat(info.getAccountValue()).isEqualTo(2500.0);
        assertThat(info.getPatientDebtSum()).isNull();
        assertThat(info.getPatientBookingAct()).hasSize(1);
        assertThat(info.getPatientBookingAct().get(0).getBookingQuantity()).isEqualTo(2);
    }

    @Test
    void getPatientInfo_missingPatientInfoNode_returnsEmpty() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json("{}"));

        assertThat(service.getPatientInfo(900001L)).isEmpty();
    }

    @Test
    void getPatientInfo_nullPatientId_returnsEmpty() {
        assertThat(service.getPatientInfo((Long) null)).isEmpty();
    }

    // ---- hospitalization ----

    @Test
    void getHospitalization_scheduleListHit_buildsDtoFromFirstEntry() {
        UUID hospId = UUID.fromString("00000000-0000-0000-0000-000000001001");
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json("""
                        {"scheduleList":[
                          {"patientID":1001,"departmentID":42}
                        ]}
                        """));

        Optional<HospitalizationDTO> result = service.getHospitalization(hospId);

        assertThat(result).isPresent();
        HospitalizationDTO dto = result.get();
        assertThat(dto.getId()).isEqualTo(hospId);
        assertThat(dto.getPatientId()).isEqualTo(1001L);
        assertThat(dto.getDiagnosis()).contains("госпіталізації");
    }

    @Test
    void getHospitalization_emptySchedule_returnsEmpty() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json("{\"scheduleList\":[]}"));

        assertThat(service.getHospitalization(
                UUID.fromString("00000000-0000-0000-0000-000000001001"))).isEmpty();
    }

    @Test
    void getHospitalization_clientException_fallsBackToEmpty() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThat(service.getHospitalization(
                UUID.fromString("00000000-0000-0000-0000-000000001001"))).isEmpty();
    }

    // ---- sendPdf / error modes ----

    @Test
    void sendPdf_returnsTrueAndAudits() {
        assertThat(service.sendPdf(UUID.randomUUID(), new byte[]{1}, "f.pdf", 1)).isTrue();
        verify(auditService).logAction(anyString(), any(), anyString(), any());
    }

    @Test
    void errorMode_notFound_throwsOnNextCall() {
        service.setErrorMode("not_found");

        assertThatThrownBy(() -> service.searchPatients(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void errorMode_unavailable_throwsOnNextCall() {
        service.setErrorMode("unavailable");

        assertThatThrownBy(() -> service.searchPatients(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void errorMode_backToNone_stopsSimulating() {
        service.setErrorMode("timeout");
        service.setErrorMode("none");

        when(misApiClient.callMethod("spzIBPatientSearch"))
                .thenReturn(json("{\"patientList\":[]}"));

        assertThat(service.searchPatients(null)).isEmpty();
    }
}
