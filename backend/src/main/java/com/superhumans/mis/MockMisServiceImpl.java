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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mis.mock-enabled", havingValue = "true", matchIfMissing = false)
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
        for (long pid = 1004; pid <= 1050; pid++) {
            String hex = String.format("00000000-0000-0000-0000-%012d", pid);
            knownHospitalizationIds.add(UUID.fromString(hex));
        }
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
        addPatient(1006L, "Шевченко Марія Олександрівна", LocalDate.of(1975, 1, 20), "F",
                "м. Київ, вул. Володимирська, 25", "380631112233", "m.shevchenko@mail.com",
                "МК-033333", "200120259003", 168, 62, "A(II)", "Rh+");
        addPatient(1007L, "Кравчук Дмитро Васильович", LocalDate.of(1968, 6, 14), "M",
                "м. Львів, вул. Франка, 12", "380504445566", "d.kravchuk@mail.com",
                "МК-044444", "140620259004", 176, 78, "O(I)", "Rh+");
        addPatient(1008L, "Бойко Наталія Петрівна", LocalDate.of(1992, 9, 30), "F",
                "м. Харків, вул. Сумська, 18", "380675556677", "n.boiko@mail.com",
                "МК-055555", "300920259005", 172, 60, "B(III)", "Rh+");
        addPatient(1009L, "Мельник Сергій Олегович", LocalDate.of(1970, 12, 5), "M",
                "м. Одеса, вул. Пушкінська, 30", "380667778899", "s.melnyk@mail.com",
                "МК-066666", "051220259006", 180, 85, "AB(IV)", "Rh+");
        addPatient(1010L, "Козак Оксана Андріївна", LocalDate.of(1988, 4, 18), "F",
                "м. Дніпро, вул. Набережна, 7", "380689990001", "o.kozak@mail.com",
                "МК-077777", "180420259007", 166, 55, "A(II)", "Rh−");
        addPatient(1011L, "Мороз Галина Іванівна", LocalDate.of(1980, 8, 25), "F",
                "м. Запоріжжя, пр. Соборний, 10", "380501113344", "g.moroz@mail.com",
                "МК-088888", "250820259008", 163, 57, "O(I)", "Rh+");
        addPatient(1012L, "Гончар Олег Петрович", LocalDate.of(1995, 2, 14), "M",
                "м. Вінниця, вул. Соборна, 5", "380632224455", "o.gonchar@mail.com",
                "МК-099999", "140220259009", 190, 92, "B(III)", "Rh−");
        addPatient(1013L, "Лисенко Анна Володимирівна", LocalDate.of(1973, 11, 8), "F",
                "м. Полтава, вул. Котляревського, 3", "380673334455", "a.lysenko@mail.com",
                "МК-101010", "081120259010", 169, 63, "AB(IV)", "Rh+");
        addPatient(1014L, "Захарчук Микола Богданович", LocalDate.of(1965, 5, 30), "M",
                "м. Рівне, вул. Незалежності, 22", "380504445566", "m.zakharchuk@mail.com",
                "МК-111111", "300520259011", 175, 80, "O(I)", "Rh+");
        addPatient(1015L, "Савчук Ірина Михайлівна", LocalDate.of(1991, 7, 19), "F",
                "м. Чернівці, вул. Головна, 15", "380685556677", "i.savchuk@mail.com",
                "МК-121212", "190720259012", 164, 54, "A(II)", "Rh+");
        addPatient(1016L, "Пономаренко Віктор Васильович", LocalDate.of(1977, 1, 2), "M",
                "м. Хмельницький, вул. Кам'янецька, 8", "380506667788", "v.ponomarenko@mail.com",
                "МК-131313", "020120259013", 183, 86, "B(III)", "Rh+");
        addPatient(1017L, "Мартинюк Людмила Олексіївна", LocalDate.of(1983, 9, 11), "F",
                "м. Житомир, вул. Київська, 30", "380507778899", "l.martyniuk@mail.com",
                "МК-141414", "110920259014", 171, 68, "AB(IV)", "Rh−");
        addPatient(1018L, "Романюк Андрій Степанович", LocalDate.of(1960, 12, 25), "M",
                "м. Тернопіль, вул. Руська, 12", "380508889900", "a.romaniuk@mail.com",
                "МК-151515", "251220259015", 174, 75, "O(I)", "Rh+");
        addPatient(1019L, "Олійник Тетяна Василівна", LocalDate.of(1993, 6, 6), "F",
                "м. Луцьк, вул. Винниченка, 7", "380509990011", "t.oliynyk@mail.com",
                "МК-161616", "060620259016", 167, 59, "A(II)", "Rh+");
        addPatient(1020L, "Федорчук Олександр Вікторович", LocalDate.of(1971, 4, 28), "M",
                "м. Івано-Франківськ, вул. Галицька, 20", "380631112244", "o.fedorchuk@mail.com",
                "МК-171717", "280420259017", 181, 83, "B(III)", "Rh−");
        addPatient(1021L, "Костенко Валентина Петрівна", LocalDate.of(1986, 10, 15), "F",
                "м. Черкаси, вул. Хрещатик, 5", "380632223355", "v.kostenko@mail.com",
                "МК-181818", "151020259018", 162, 52, "O(I)", "Rh+");
        addPatient(1022L, "Павлюк Богдан Мирославович", LocalDate.of(1967, 3, 22), "M",
                "м. Ужгород, вул. Корзо, 10", "380503334466", "b.pavlyuk@mail.com",
                "МК-191919", "220320259019", 177, 79, "AB(IV)", "Rh+");
        addPatient(1023L, "Дорошенко Катерина Сергіївна", LocalDate.of(1994, 8, 9), "F",
                "м. Миколаїв, вул. Адміральська, 14", "380504445577", "k.doroshenko@mail.com",
                "МК-202020", "090820259020", 170, 61, "A(II)", "Rh−");
        addPatient(1024L, "Гаврилюк Петро Іванович", LocalDate.of(1958, 7, 1), "M",
                "м. Суми, вул. Харківська, 25", "380505556688", "p.gavrylyuk@mail.com",
                "МК-212121", "010720259021", 172, 74, "O(I)", "Rh+");
        addPatient(1025L, "Тарасенко Ольга Миколаївна", LocalDate.of(1989, 2, 28), "F",
                "м. Кропивницький, вул. Велика, 3", "380506667799", "o.tarasenko@mail.com",
                "МК-222222", "280220259022", 165, 56, "B(III)", "Rh+");
        addPatient(1026L, "Ващенко Юрій Олександрович", LocalDate.of(1974, 11, 30), "M",
                "м. Чернігів, вул. Шевченка, 18", "380637778800", "y.vashchenko@mail.com",
                "МК-232323", "301120259023", 186, 91, "AB(IV)", "Rh−");
        addPatient(1027L, "Демченко Надія Павлівна", LocalDate.of(1996, 5, 16), "F",
                "м. Кривий Ріг, пр. Миру, 40", "380508889911", "n.demchenko@mail.com",
                "МК-242424", "160520259024", 168, 58, "A(II)", "Rh+");
        addPatient(1028L, "Гриценко Максим Віталійович", LocalDate.of(1963, 9, 5), "M",
                "м. Маріуполь, вул. Митрополитська, 6", "380509990022", "m.grytsenko@mail.com",
                "МК-252525", "050920259025", 179, 84, "B(III)", "Rh+");
        addPatient(1029L, "Данилюк Світлана Романівна", LocalDate.of(1981, 12, 12), "F",
                "м. Біла Церква, вул. Ярослава Мудрого, 9", "380631113344", "s.danylyuk@mail.com",
                "МК-262626", "121220259026", 164, 53, "O(I)", "Rh+");
        addPatient(1030L, "Семенюк Володимир Дмитрович", LocalDate.of(1969, 6, 20), "M",
                "м. Херсон, вул. Університетська, 11", "380672224455", "v.semenyuk@mail.com",
                "МК-272727", "200620259027", 184, 87, "A(II)", "Rh+");
        addPatient(1031L, "Іванова Олександра Вікторівна", LocalDate.of(1998, 4, 3), "F",
                "м. Київ, вул. Харківське шосе, 56", "380503335566", "o.ivanova@mail.com",
                "МК-282828", "030420259028", 171, 63, "AB(IV)", "Rh+");
        addPatient(1032L, "Петров Артем Ігорович", LocalDate.of(1979, 8, 17), "M",
                "м. Київ, вул. Саксаганського, 100", "380684446677", "a.petrov@mail.com",
                "МК-292929", "170820259029", 178, 76, "O(I)", "Rh−");
        addPatient(1033L, "Ковальова Лариса Миколаївна", LocalDate.of(1984, 3, 9), "F",
                "м. Львів, вул. Листопадового Чину, 4", "380505557788", "l.kovalova@mail.com",
                "МК-303030", "090320259030", 166, 57, "B(III)", "Rh+");
        addPatient(1034L, "Сидоров Денис Олегович", LocalDate.of(1990, 10, 22), "M",
                "м. Одеса, вул. Канатна, 15", "380506668899", "d.sydorov@mail.com",
                "МК-313131", "221020259031", 188, 82, "A(II)", "Rh+");
        addPatient(1035L, "Ткачова Олена Валеріївна", LocalDate.of(1976, 2, 11), "F",
                "м. Дніпро, вул. Титова, 22", "380507779900", "o.tkachova@mail.com",
                "МК-323232", "110220259032", 169, 60, "AB(IV)", "Rh−");
        addPatient(1036L, "Шевчук Руслан Михайлович", LocalDate.of(1966, 7, 4), "M",
                "м. Запоріжжя, вул. Сталеварів, 8", "380638880011", "r.shevchuk@mail.com",
                "МК-333333", "040720259033", 180, 81, "O(I)", "Rh+");
        addPatient(1037L, "Марчук Віра Петрівна", LocalDate.of(1987, 12, 1), "F",
                "м. Харків, вул. Клочківська, 45", "380509991122", "v.marchuk@mail.com",
                "МК-343434", "011220259034", 173, 64, "B(III)", "Rh+");
        addPatient(1038L, "Яремчук Олег Степанович", LocalDate.of(1959, 9, 15), "M",
                "м. Тернопіль, вул. Грушевського, 6", "380501112200", "o.yaremchuk@mail.com",
                "МК-353535", "150920259035", 170, 72, "A(II)", "Rh+");
        addPatient(1039L, "Гордієнко Наталія Віталіївна", LocalDate.of(1997, 1, 28), "F",
                "м. Рівне, вул. Соборна, 14", "380632223300", "n.gordienko@mail.com",
                "МК-363636", "280120259036", 167, 55, "AB(IV)", "Rh+");
        addPatient(1040L, "Антонюк Віталій Миколайович", LocalDate.of(1972, 6, 8), "M",
                "м. Луцьк, вул. Лесі Українки, 12", "380673334411", "v.antonyuk@mail.com",
                "МК-373737", "080620259037", 176, 77, "O(I)", "Rh−");
        addPatient(1041L, "Мазур Юлія Андріївна", LocalDate.of(1993, 11, 10), "F",
                "м. Івано-Франківськ, вул. Незалежності, 30", "380684445522", "y.mazur@mail.com",
                "МК-383838", "101120259038", 165, 56, "B(III)", "Rh+");
        addPatient(1042L, "Литвин Олександр Павлович", LocalDate.of(1961, 4, 12), "M",
                "м. Вінниця, вул. Пирогова, 50", "380695556633", "o.lytvyn@mail.com",
                "МК-393939", "120420259039", 173, 73, "A(II)", "Rh+");
        addPatient(1043L, "Кузьмін Оксана Борисівна", LocalDate.of(1986, 8, 23), "F",
                "м. Полтава, вул. Європейська, 28", "380506667744", "o.kuzmin@mail.com",
                "МК-404040", "230820259040", 170, 62, "AB(IV)", "Rh+");
        addPatient(1044L, "Ковальчук Андрій Іванович", LocalDate.of(1975, 12, 30), "M",
                "м. Чернівці, вул. Університетська, 16", "380507778855", "a.kovalchuk@mail.com",
                "МК-414141", "301220259041", 182, 89, "O(I)", "Rh+");
        addPatient(1045L, "Герасименко Інна Олегівна", LocalDate.of(1995, 5, 5), "F",
                "м. Житомир, вул. Перемоги, 22", "380508889966", "i.gerasymenko@mail.com",
                "МК-424242", "050520259042", 168, 59, "B(III)", "Rh+");
        addPatient(1046L, "Ткачук Євген Вікторович", LocalDate.of(1968, 10, 19), "M",
                "м. Миколаїв, вул. Потьомкіна, 5", "380639990077", "y.tkachuk@mail.com",
                "МК-434343", "191020259043", 175, 78, "A(II)", "Rh−");
        addPatient(1047L, "Науменко Тетяна Ігорівна", LocalDate.of(1982, 7, 7), "F",
                "м. Хмельницький, вул. Проскурівська, 8", "380551112288", "t.naumenko@mail.com",
                "МК-444444", "070720259044", 166, 54, "AB(IV)", "Rh+");
        addPatient(1048L, "Левченко Вадим Сергійович", LocalDate.of(1970, 3, 15), "M",
                "м. Суми, вул. Гагаріна, 30", "380502223399", "v.levchenko@mail.com",
                "МК-454545", "150320259045", 181, 85, "O(I)", "Rh+");
        addPatient(1049L, "Бондарєва Аліна Олександрівна", LocalDate.of(1991, 9, 9), "F",
                "м. Черкаси, вул. Благовісна, 17", "380503334400", "a.bondareva@mail.com",
                "МК-464646", "090920259046", 172, 61, "B(III)", "Rh−");
        addPatient(1050L, "Руденко Ігор Васильович", LocalDate.of(1964, 1, 27), "M",
                "м. Кривий Ріг, вул. Металургів, 12", "380504445511", "i.rudenko@mail.com",
                "МК-474747", "270120259047", 177, 80, "A(II)", "Rh+");
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
        // Extract patient ID from the last 12 decimal digits of the hospitalization UUID
        String nodePart = hospitalizationId.toString().substring(24);
        long patientId = Long.parseLong(nodePart);
        PatientDTO patient = patients.get(patientId);
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

    @Override
    public boolean sendPdf(UUID clinicalDayId, byte[] pdfContent, String fileName, int version) {
        checkErrors();
        auditService.logAction("MIS", clinicalDayId, "SEND_PDF", getCurrentUserId());
        return true;
    }
}
