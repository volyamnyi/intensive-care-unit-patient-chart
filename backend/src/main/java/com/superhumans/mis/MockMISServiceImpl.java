package com.superhumans.mis;

import com.superhumans.mis.dto.DictionaryItemDTO;
import com.superhumans.mis.dto.DocumentDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.mis.dto.UserMISDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MockMISServiceImpl implements MISService {

    private final List<PatientDTO> mockPatients = new ArrayList<>();
    private final List<UserMISDTO> mockUsers = new ArrayList<>();

    @PostConstruct
    void init() {
        mockPatients.add(PatientDTO.builder()
                .patientID(1001).patientName("Петренко Іван Сергійович")
                .patientBirthDate(LocalDate.of(1978, 3, 15))
                .patientSexCode("M").patientAddress("м. Київ, вул. Хрещатик, 15")
                .patientPhone("380501234567").patientEmail("ivan.petrenko@mail.com")
                .patientExternalID1("МК-001234").patientExternalID2("301020251234")
                .patientHeight(178).patientWeight(82)
                .bloodGroup("A(II)").rhFactor("Rh+")
                .build());

        mockPatients.add(PatientDTO.builder()
                .patientID(1002).patientName("Коваленко Олена Вікторівна")
                .patientBirthDate(LocalDate.of(1985, 11, 22))
                .patientSexCode("F").patientAddress("м. Львів, вул. Шевченка, 8")
                .patientPhone("380671112233").patientEmail("olena.kov@mail.com")
                .patientExternalID1("МК-005678").patientExternalID2("150320259876")
                .patientHeight(165).patientWeight(58)
                .bloodGroup("B(III)").rhFactor("Rh+")
                .build());

        mockPatients.add(PatientDTO.builder()
                .patientID(1003).patientName("Сидоренко Василь Петрович")
                .patientBirthDate(LocalDate.of(1962, 7, 8))
                .patientSexCode("M").patientAddress("м. Харків, пр. Науки, 42")
                .patientPhone("380631234567").patientEmail("vasyl.syd@mail.com")
                .patientExternalID1("МК-009012").patientExternalID2("070820253456")
                .patientHeight(182).patientWeight(90)
                .bloodGroup("O(I)").rhFactor("Rh−")
                .build());

        // 20 test patients
        addPatient(1004, "Бондаренко Тетяна Миколаївна", LocalDate.of(1990, 5, 12), "F", "м. Одеса, вул. Дерибасівська, 20", "380501112233", "t.bond@mail.com", "МК-011111", "120520259001", 170, 65, "A(II)", "Rh+");
        addPatient(1005, "Ткачук Андрій Вікторович", LocalDate.of(1982, 9, 3), "M", "м. Дніпро, пр. Яворницького, 55", "380632223344", "and.tkach@mail.com", "МК-022222", "030920259002", 185, 88, "AB(IV)", "Rh−");
        addPatient(1006, "Романенко Ольга Петрівна", LocalDate.of(1975, 12, 28), "F", "м. Запоріжжя, вул. Соборна, 10", "380504445566", "olga.rom@mail.com", "МК-033333", "281220259003", 163, 72, "O(I)", "Rh+");
        addPatient(1007, "Савенко Дмитро Олександрович", LocalDate.of(1995, 2, 14), "M", "м. Вінниця, вул. Соборна, 30", "380675556677", "dm.sav@mail.com", "МК-044444", "140220259004", 180, 78, "B(III)", "Rh+");
        addPatient(1008, "Павленко Ірина Олегівна", LocalDate.of(1988, 7, 19), "F", "м. Полтава, вул. Європейська, 5", "380956667788", "iryna.pav@mail.com", "МК-055555", "190720259005", 167, 60, "A(II)", "Rh−");
        addPatient(1009, "Грищенко Сергій Володимирович", LocalDate.of(1970, 11, 1), "M", "м. Чернігів, пр. Перемоги, 12", "380937778899", "serg.gry@mail.com", "МК-066666", "011120259006", 176, 85, "O(I)", "Rh+");
        addPatient(1010, "Кравченко Наталія Романівна", LocalDate.of(1992, 4, 8), "F", "м. Суми, вул. Харківська, 18", "380508889900", "nat.krav@mail.com", "МК-077777", "080420259007", 160, 55, "B(III)", "Rh+");
        addPatient(1011, "Мельниченко Віталій Ігорович", LocalDate.of(1980, 8, 25), "M", "м. Житомир, вул. Київська, 7", "380679990011", "vit.mel@mail.com", "МК-088888", "250820259008", 183, 92, "AB(IV)", "Rh+");
        addPatient(1012, "Шевчук Катерина Василівна", LocalDate.of(1986, 1, 30), "F", "м. Рівне, вул. Соборна, 22", "380631001122", "kat.shev@mail.com", "МК-099999", "300120259009", 168, 63, "A(II)", "Rh+");
        addPatient(1013, "Лисенко Михайло Олексійович", LocalDate.of(1973, 6, 17), "M", "м. Хмельницький, вул. Проскурівська, 15", "380502223344", "mich.lis@mail.com", "МК-101010", "170620259010", 179, 80, "O(I)", "Rh−");
        addPatient(1014, "Гаврилюк Анна Богданівна", LocalDate.of(1998, 3, 5), "F", "м. Тернопіль, вул. Руська, 9", "380673334455", "anna.gav@mail.com", "МК-111111", "050320259011", 165, 57, "B(III)", "Rh+");
        addPatient(1015, "Федорчук Олександр Юрійович", LocalDate.of(1968, 10, 10), "M", "м. Луцьк, вул. Винниченка, 3", "380954445566", "olex.fed@mail.com", "МК-121212", "101020259012", 175, 95, "A(II)", "Rh+");
        addPatient(1016, "Мороз Юлія Віталіївна", LocalDate.of(1991, 12, 22), "F", "м. Ужгород, пл. Корятовича, 6", "380935556677", "yuli.mor@mail.com", "МК-131313", "221220259013", 162, 59, "AB(IV)", "Rh−");
        addPatient(1017, "Даниленко Максим Сергійович", LocalDate.of(1984, 9, 14), "M", "м. Кропивницький, вул. Велика Перспективна, 28", "380507778899", "max.dan@mail.com", "МК-141414", "140920259014", 188, 86, "O(I)", "Rh+");
        addPatient(1018, "Олійник Віра Андріївна", LocalDate.of(1977, 2, 28), "F", "м. Івано-Франківськ, вул. Незалежності, 14", "380638889900", "vira.oli@mail.com", "МК-151515", "280220259015", 166, 70, "B(III)", "Rh+");
        addPatient(1019, "Ковальчук Роман Петрович", LocalDate.of(1996, 7, 7), "M", "м. Черкаси, бул. Шевченка, 40", "380509990011", "roman.kov@mail.com", "МК-161616", "070720259016", 181, 77, "A(II)", "Rh+");
        addPatient(1020, "Семенюк Людмила Іванівна", LocalDate.of(1989, 5, 20), "F", "м. Миколаїв, пр. Центральний, 25", "380671001122", "lyuda.sem@mail.com", "МК-171717", "200520259017", 164, 61, "O(I)", "Rh−");
        addPatient(1021, "Тарасенко Олег Володимирович", LocalDate.of(1971, 8, 3), "M", "м. Херсон, вул. Ушакова, 11", "380632112233", "oleh.tar@mail.com", "МК-181818", "030820259018", 177, 89, "AB(IV)", "Rh+");
        addPatient(1022, "Пономаренко Оксана В'ячеславівна", LocalDate.of(1983, 11, 16), "F", "м. Чернівці, вул. Головна, 35", "380562223344", "oks.pon@mail.com", "МК-191919", "161120259019", 169, 68, "B(III)", "Rh+");
        addPatient(1023, "Зінченко Артем Олегович", LocalDate.of(1993, 4, 1), "M", "м. Київ, вул. Велика Васильківська, 50", "380503334455", "artem.zin@mail.com", "МК-202020", "010420259020", 184, 81, "A(II)", "Rh+");

        mockUsers.add(UserMISDTO.builder()
                .userLogin("doctor1").userName("Олександр Мельник")
                .userShortName("Мельник О.").userSpecialityCode("101")
                .userSpecialityName("Лікар-анестезіолог")
                .userEmail("melnyk@hospital.ua").userPhone("380501111111")
                .build());

        mockUsers.add(UserMISDTO.builder()
                .userLogin("doctor2").userName("Наталія Бойко")
                .userShortName("Бойко Н.").userSpecialityCode("101")
                .userSpecialityName("Лікар-анестезіолог")
                .userEmail("boyko@hospital.ua").userPhone("380502222222")
                .build());

        mockUsers.add(UserMISDTO.builder()
                .userLogin("nurse1").userName("Олена Ткаченко")
                .userShortName("Ткаченко О.").userSpecialityCode("201")
                .userSpecialityName("Медична сестра ВАІТ")
                .userEmail("tkachenko@hospital.ua").userPhone("380503333333")
                .build());

        mockUsers.add(UserMISDTO.builder()
                .userLogin("nurse2").userName("Марія Кравчук")
                .userShortName("Кравчук М.").userSpecialityCode("201")
                .userSpecialityName("Медична сестра ВАІТ")
                .userEmail("kravchuk@hospital.ua").userPhone("380504444444")
                .build());

        mockUsers.add(UserMISDTO.builder()
                .userLogin("head1").userName("Василь Гончарук")
                .userShortName("Гончарук В.").userSpecialityCode("301")
                .userSpecialityName("Завідувач ВАІТ")
                .userEmail("goncharuk@hospital.ua").userPhone("380505555555")
                .build());

        mockUsers.add(UserMISDTO.builder()
                .userLogin("admin").userName("Адмін Системи")
                .userShortName("Адмін").userSpecialityCode("999")
                .userSpecialityName("Адміністратор")
                .userEmail("admin@hospital.ua").userPhone("380506666666")
                .build());
    }

    private void addPatient(Integer id, String name, LocalDate birthDate, String sex, String address,
                            String phone, String email, String extId1, String extId2,
                            Integer height, Integer weight, String bloodGroup, String rhFactor) {
        mockPatients.add(PatientDTO.builder()
                .patientID(id).patientName(name)
                .patientBirthDate(birthDate)
                .patientSexCode(sex).patientAddress(address)
                .patientPhone(phone).patientEmail(email)
                .patientExternalID1(extId1).patientExternalID2(extId2)
                .patientHeight(height).patientWeight(weight)
                .bloodGroup(bloodGroup).rhFactor(rhFactor)
                .build());
    }

    @Override
    public List<PatientDTO> searchPatients(String name, String phone, String externalId) {
        return mockPatients.stream()
                .filter(p -> name == null || p.getPatientName().toLowerCase().contains(name.toLowerCase()))
                .filter(p -> phone == null || p.getPatientPhone().contains(phone))
                .filter(p -> externalId == null || p.getPatientExternalID1().contains(externalId)
                        || p.getPatientExternalID2().contains(externalId))
                .collect(Collectors.toList());
    }

    @Override
    public PatientDTO getPatientInfo(Integer patientId, String documentSequenceNumber) {
        return mockPatients.stream()
                .filter(p -> p.getPatientID().equals(patientId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public UserMISDTO getUserDetails(String login, String specialityCode) {
        return mockUsers.stream()
                .filter(u -> u.getUserLogin().equals(login))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<DocumentDTO> getPatientDocuments(Integer patientId, LocalDate start, LocalDate end) {
        return List.of();
    }

    @Override
    public List<DictionaryItemDTO> getDocumentApproveStatuses() {
        return List.of(
                new DictionaryItemDTO("CREATED", "Створено"),
                new DictionaryItemDTO("SIGNED_DOCTOR", "Підписано лікарем"),
                new DictionaryItemDTO("SENT_TO_MIS", "Відправлено в МІС"),
                new DictionaryItemDTO("APPROVED", "Затверджено")
        );
    }
}
