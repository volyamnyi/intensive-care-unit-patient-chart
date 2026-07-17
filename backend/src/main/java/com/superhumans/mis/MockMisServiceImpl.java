package com.superhumans.mis;

import com.superhumans.mis.dto.*;
import com.superhumans.service.AuditService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MockMisServiceImpl implements MisService {

    final AuditService auditService;

    boolean simulateErrors = false;
    String errorMode = "none";

    public void setErrorMode(String mode) {
        this.errorMode = mode;
        this.simulateErrors = !"none".equals(mode);
    }

    final Map<Long, PatientDTO> patients = new LinkedHashMap<>();
    final Set<UUID> knownHospitalizationIds = new HashSet<>();
    final Map<Long, UserMisDTO> users = new LinkedHashMap<>();
    final Map<Long, DepartmentDTO> departments = new LinkedHashMap<>();
    final Map<Long, List<UserMisDTO>> departmentUsers = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        initPatients();
        initUsers();
        initDepartments();
    }

    void initPatients() {
        knownHospitalizationIds.add(UUID.fromString("00000000-0000-0000-0000-000000001001"));
        knownHospitalizationIds.add(UUID.fromString("00000000-0000-0000-0000-000000001002"));
        addPatient(1001L, "Петренко Іван Сергійович", LocalDate.of(1978, 3, 15), "M",
                "м. Київ, вул. Хрещатик, 15", "380501234567", "ivan.petrenko@mail.com",
                "МК-001234", "301020251234", 178, 82, "A(II)", "Rh+");
        addPatient(1002L, "Коваленко Олена Вікторівна", LocalDate.of(1985, 11, 22), "F",
                "м. Львів, вул. Шевченка, 8", "380671112233", "olena.kov@mail.com",
                "МК-005678", "150320259876", 165, 58, "B(III)", "Rh+");
        addPatient(1003L, "Сидоренко Василь Петрович", LocalDate.of(1962, 7, 8), "M",
                "м. Харків, пр. Науки, 42", "380631234567", "vasyl.syd@mail.com",
                "МК-009012", "070820253456", 182, 90, "O(I)", "Rh−");
        addPatient(1004L, "Бондаренко Тетяна Миколаївна", LocalDate.of(1990, 5, 12), "F",
                "м. Одеса, вул. Дерибасівська, 20", "380501112233", "t.bond@mail.com",
                "МК-011111", "120520259001", 170, 65, "A(II)", "Rh+");
        addPatient(1005L, "Ткачук Андрій Вікторович", LocalDate.of(1982, 9, 3), "M",
                "м. Дніпро, пр. Яворницького, 55", "380632223344", "and.tkach@mail.com",
                "МК-022222", "030920259002", 185, 88, "AB(IV)", "Rh−");
    }

    void addPatient(Long id, String name, LocalDate birthDate, String sex, String address,
                            String phone, String email, String extId1, String extId2,
                            Integer height, Integer weight, String bloodGroup, String rhFactor) {
        patients.put(id, PatientDTO.builder()
                .id(id).fullName(name).birthDate(birthDate).sexCode(sex)
                .address(address).phone(phone).email(email)
                .externalId1(extId1).externalId2(extId2)
                .height(height).weight(weight)
                .bloodGroup(bloodGroup).rhFactor(rhFactor)
                .build());
    }

    void initUsers() {
        addUser(11L, "doctor1", "Олександр Мельник", "Мельник О.",
                "101", "Лікар-анестезіолог", "melnyk@hospital.ua", "380501111111");
        addUser(12L, "doctor2", "Наталія Бойко", "Бойко Н.",
                "101", "Лікар-анестезіолог", "boyko@hospital.ua", "380502222222");
        addUser(13L, "nurse1", "Олена Ткаченко", "Ткаченко О.",
                "201", "Медична сестра ВАІТ", "tkachenko@hospital.ua", "380503333333");
        addUser(14L, "nurse2", "Марія Кравчук", "Кравчук М.",
                "201", "Медична сестра ВАІТ", "kravchuk@hospital.ua", "380504444444");
        addUser(15L, "head1", "Василь Гончарук", "Гончарук В.",
                "301", "Завідувач ВАІТ", "goncharuk@hospital.ua", "380505555555");
        addUser(16L, "admin", "Адмін Системи", "Адмін",
                "999", "Адміністратор", "admin@hospital.ua", "380506666666");
    }

    void addUser(Long id, String login, String name, String shortName,
                         String specCode, String specName, String email, String phone) {
        users.put(id, UserMisDTO.builder()
                .id(id).login(login).fullName(name).shortName(shortName)
                .specialityCode(specCode).specialityName(specName)
                .email(email).phone(phone)
                .build());
    }

    void initDepartments() {
        DepartmentDTO icu = DepartmentDTO.builder()
                .id(1L).name("Відділення анестезіології та інтенсивної терапії").code("VAIT").build();
        departments.put(icu.getId(), icu);
        DepartmentDTO surgery = DepartmentDTO.builder()
                .id(2L).name("Хірургічне відділення").code("SURG").build();
        departments.put(surgery.getId(), surgery);

        departmentUsers.put(icu.getId(), List.of(
                users.get(11L), users.get(12L),
                users.get(13L), users.get(14L),
                users.get(15L)));
        departmentUsers.put(surgery.getId(), List.of(
                users.get(11L), users.get(15L)));
    }

    Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof Long id) {
            return id;
        }
        return null;
    }

    void checkErrors() {
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
        Optional<PatientDTO> result = Optional.ofNullable(patients.get(patientId));
        auditService.logAction("MIS", null, "GET_PATIENT", getCurrentUserId());
        return result;
    }

    @Override
    public Optional<HospitalizationDTO> getHospitalization(UUID hospitalizationId) {
        checkErrors();
        if (hospitalizationId == null || !knownHospitalizationIds.contains(hospitalizationId)) {
            return Optional.empty();
        }
        // Map hospitalization to a patient by using the last 4 digits
        PatientDTO patient = patients.values().stream().findFirst().orElse(null);
        if (patient == null) return Optional.empty();

        DepartmentDTO dept = departments.values().iterator().next();
        auditService.logAction("MIS", null, "GET_HOSPITALIZATION", getCurrentUserId());
        return Optional.of(HospitalizationDTO.builder()
                .id(hospitalizationId)
                .patientId(patient.getId())
                .departmentId(dept.getId())
                .admissionDate(LocalDateTime.now().minusDays(3))
                .diagnosis("Діагноз при госпіталізації")
                .departmentName(dept.getName())
                .room("101")
                .bed("A")
                .build());
    }

    @Override
    public Optional<UserMisDTO> getUser(Long userId) {
        checkErrors();
        Optional<UserMisDTO> result = Optional.ofNullable(users.get(userId));
        auditService.logAction("MIS", null, "GET_USER", getCurrentUserId());
        return result;
    }

    @Override
    public List<UserMisDTO> getDepartmentUsers(Long departmentId) {
        checkErrors();
        List<UserMisDTO> result = departmentUsers.getOrDefault(departmentId, List.of());
        auditService.logAction("MIS", null, "GET_DEPARTMENT_USERS", getCurrentUserId());
        return result;
    }

    @Override
    public List<DepartmentDTO> getDepartments() {
        checkErrors();
        List<DepartmentDTO> result = List.copyOf(departments.values());
        auditService.logAction("MIS", null, "GET_DEPARTMENTS", getCurrentUserId());
        return result;
    }

    @Override
    public List<PatientDTO> searchPatients(String query) {
        checkErrors();
        auditService.logAction("MIS", null, "SEARCH_PATIENTS", getCurrentUserId());
        if (query == null || query.isBlank()) {
            return List.copyOf(patients.values());
        }
        String lower = query.toLowerCase();
        return patients.values().stream()
                .filter(p -> p.getFullName().toLowerCase().contains(lower)
                        || p.getExternalId1().toLowerCase().contains(lower)
                        || p.getPhone().contains(lower))
                .collect(Collectors.toList());
    }

    @Override
    public List<DictionaryItemDTO> getDictionary(String dictionaryName) {
        checkErrors();
        auditService.logAction("MIS", null, "GET_DICTIONARY", getCurrentUserId());
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
            default -> List.of();
        };
    }
}
