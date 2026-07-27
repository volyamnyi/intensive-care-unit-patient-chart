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
    final List<MedicineMisDTO> medicineCatalog = new ArrayList<>();
    final Map<Long, List<AllergyMisDTO>> allergyData = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        initPatients();
        initUsers();
        initDepartments();
        initMedicineCatalog();
        initAllergies();
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

        // Seed surgery patients (2001-2020) for prescription dashboard
        addPatient(2001L, "Василенко Петро Іванович", LocalDate.of(1965, 3, 12), "M",
                "м. Львів, вул. Хірургічна, 1", "380501111001", "p.vasylenko@mail.com",
                "МК-2001", "120320252001", 178, 82, "A(II)", "Rh+");
        addPatient(2002L, "Ковальчук Олена Миколаївна", LocalDate.of(1972, 7, 25), "F",
                "м. Львів, вул. Хірургічна, 2", "380502222002", "o.kovalchuk@mail.com",
                "МК-2002", "250720252002", 165, 58, "B(III)", "Rh+");
        addPatient(2003L, "Шевчук Андрій Степанович", LocalDate.of(1960, 11, 8), "M",
                "м. Львів, вул. Хірургічна, 3", "380503333003", "a.shevchuk@mail.com",
                "МК-2003", "081120252003", 182, 90, "O(I)", "Rh−");
        addPatient(2004L, "Мельник Тетяна Володимирівна", LocalDate.of(1985, 5, 18), "F",
                "м. Львів, вул. Хірургічна, 4", "380504444004", "t.melnyk@mail.com",
                "МК-2004", "180520252004", 170, 65, "A(II)", "Rh+");
        addPatient(2005L, "Бойко Василь Михайлович", LocalDate.of(1978, 9, 30), "M",
                "м. Львів, вул. Хірургічна, 5", "380505555005", "v.boyko@mail.com",
                "МК-2005", "300920252005", 185, 88, "AB(IV)", "Rh−");
        addPatient(2006L, "Кравченко Наталія Олегівна", LocalDate.of(1990, 1, 14), "F",
                "м. Львів, вул. Хірургічна, 6", "380506666006", "n.kravchenko@mail.com",
                "МК-2006", "140120252006", 168, 62, "A(II)", "Rh+");
        addPatient(2007L, "Захарченко Ігор Борисович", LocalDate.of(1968, 12, 3), "M",
                "м. Львів, вул. Хірургічна, 7", "380507777007", "i.zakharchenko@mail.com",
                "МК-2007", "031220252007", 176, 78, "O(I)", "Rh+");
        addPatient(2008L, "Гриценко Марія Андріївна", LocalDate.of(1982, 4, 22), "F",
                "м. Львів, вул. Хірургічна, 8", "380508888008", "m.hrytsenko@mail.com",
                "МК-2008", "220420252008", 163, 55, "B(III)", "Rh+");
        addPatient(2009L, "Даниленко Сергій Павлович", LocalDate.of(1975, 8, 17), "M",
                "м. Львів, вул. Хірургічна, 9", "380509999009", "s.danylenko@mail.com",
                "МК-2009", "170820252009", 180, 85, "AB(IV)", "Rh+");
        addPatient(2010L, "Литвиненко Оксана Василівна", LocalDate.of(1988, 6, 9), "F",
                "м. Львів, вул. Хірургічна, 10", "380501010010", "o.lytvynenko@mail.com",
                "МК-2010", "090620252010", 172, 60, "A(II)", "Rh−");
        addPatient(2011L, "Поліщук Віктор Іванович", LocalDate.of(1970, 2, 28), "M",
                "м. Львів, вул. Хірургічна, 11", "380501111011", "v.polishchuk@mail.com",
                "МК-2011", "280220252011", 177, 80, "O(I)", "Rh+");
        addPatient(2012L, "Бондаренко Ганна Петрівна", LocalDate.of(1992, 10, 5), "F",
                "м. Львів, вул. Хірургічна, 12", "380501212012", "h.bondarenko@mail.com",
                "МК-2012", "051020252012", 166, 57, "B(III)", "Rh+");
        addPatient(2013L, "Ткачук Максим Володимирович", LocalDate.of(1963, 7, 19), "M",
                "м. Львів, вул. Хірургічна, 13", "380501313013", "m.tkachuk@mail.com",
                "МК-2013", "190720252013", 183, 86, "AB(IV)", "Rh−");
        addPatient(2014L, "Сидоренко Валентина Михайлівна", LocalDate.of(1979, 11, 1), "F",
                "м. Львів, вул. Хірургічна, 14", "380501414014", "v.sydorenko@mail.com",
                "МК-2014", "011120252014", 169, 63, "A(II)", "Rh+");
        addPatient(2015L, "Гончарук Олег Дмитрович", LocalDate.of(1971, 5, 14), "M",
                "м. Львів, вул. Хірургічна, 15", "380501515015", "o.honcharuk@mail.com",
                "МК-2015", "140520252015", 175, 75, "O(I)", "Rh+");
        addPatient(2016L, "Кузьменко Світлана Ігорівна", LocalDate.of(1986, 8, 23), "F",
                "м. Львів, вул. Хірургічна, 16", "380501616016", "s.kuzmenko@mail.com",
                "МК-2016", "230820252016", 171, 68, "AB(IV)", "Rh+");
        addPatient(2017L, "Іваненко Дмитро Олегович", LocalDate.of(1973, 12, 31), "M",
                "м. Львів, вул. Хірургічна, 17", "380501717017", "d.ivanenko@mail.com",
                "МК-2017", "311220252017", 179, 83, "A(II)", "Rh−");
        addPatient(2018L, "Савченко Алла Вікторівна", LocalDate.of(1984, 3, 7), "F",
                "м. Львів, вул. Хірургічна, 18", "380501818018", "a.savchenko@mail.com",
                "МК-2018", "070320252018", 164, 56, "B(III)", "Rh+");
        addPatient(2019L, "Марченко Юрій Степанович", LocalDate.of(1967, 4, 16), "M",
                "м. Львів, вул. Хірургічна, 19", "380501919019", "y.marchenko@mail.com",
                "МК-2019", "160420252019", 181, 84, "O(I)", "Rh+");
        addPatient(2020L, "Костенко Надія Петрівна", LocalDate.of(1994, 9, 2), "F",
                "м. Львів, вул. Хірургічна, 20", "380502020020", "n.kostenko@mail.com",
                "МК-2020", "020920252020", 167, 59, "A(II)", "Rh+");

        // Seed rehabilitation patients (2021-2040) for prescription dashboard
        addPatient(2021L, "Павленко Іван Семенович", LocalDate.of(1955, 6, 18), "M",
                "м. Львів, вул. Реабілітаційна, 1", "380502121021", "i.pavlenko@mail.com",
                "МК-2021", "180620252021", 172, 72, "A(II)", "Rh+");
        addPatient(2022L, "Макаренко Олена Дмитрівна", LocalDate.of(1962, 9, 4), "F",
                "м. Львів, вул. Реабілітаційна, 2", "380502222022", "o.makarenko@mail.com",
                "МК-2022", "040920252022", 160, 55, "B(III)", "Rh+");
        addPatient(2023L, "Тимошенко Богдан Федорович", LocalDate.of(1958, 1, 15), "M",
                "м. Львів, вул. Реабілітаційна, 3", "380502323023", "b.tymoshenko@mail.com",
                "МК-2023", "150120252023", 178, 82, "O(I)", "Rh−");
        addPatient(2024L, "Левченко Віра Григорівна", LocalDate.of(1970, 11, 22), "F",
                "м. Львів, вул. Реабілітаційна, 4", "380502424024", "v.levchenko@mail.com",
                "МК-2024", "221120252024", 163, 60, "AB(IV)", "Rh+");
        addPatient(2025L, "Остапенко Григорій Якович", LocalDate.of(1948, 3, 8), "M",
                "м. Львів, вул. Реабілітаційна, 5", "380502525025", "h.ostapenko@mail.com",
                "МК-2025", "080320252025", 175, 78, "A(II)", "Rh+");
        addPatient(2026L, "Кривенко Любов Антонівна", LocalDate.of(1965, 7, 31), "F",
                "м. Львів, вул. Реабілітаційна, 6", "380502626026", "l.kryvenko@mail.com",
                "МК-2026", "310720252026", 161, 54, "O(I)", "Rh+");
        addPatient(2027L, "Демченко Сергій Тарасович", LocalDate.of(1972, 5, 11), "M",
                "м. Львів, вул. Реабілітаційна, 7", "380502727027", "s.demchenko@mail.com",
                "МК-2027", "110520252027", 180, 85, "B(III)", "Rh−");
        addPatient(2028L, "Яценко Раїса Іллівна", LocalDate.of(1952, 8, 19), "F",
                "м. Львів, вул. Реабілітаційна, 8", "380502828028", "r.yatsenko@mail.com",
                "МК-2028", "190820252028", 158, 52, "AB(IV)", "Rh+");
        addPatient(2029L, "Філоненко Артур Леонідович", LocalDate.of(1968, 2, 27), "M",
                "м. Львів, вул. Реабілітаційна, 9", "380502929029", "a.filonenko@mail.com",
                "МК-2029", "270220252029", 182, 88, "A(II)", "Rh+");
        addPatient(2030L, "Гаврилюк Катерина Павлівна", LocalDate.of(1975, 10, 13), "F",
                "м. Львів, вул. Реабілітаційна, 10", "380503030030", "k.havryliuk@mail.com",
                "МК-2030", "131020252030", 165, 63, "O(I)", "Rh−");
        addPatient(2031L, "Семенюк Степан Ілліч", LocalDate.of(1950, 12, 1), "M",
                "м. Львів, вул. Реабілітаційна, 11", "380503131031", "s.semeniuk@mail.com",
                "МК-2031", "011220252031", 170, 70, "B(III)", "Rh+");
        addPatient(2032L, "Гордієнко Ніна Семенівна", LocalDate.of(1960, 4, 6), "F",
                "м. Львів, вул. Реабілітаційна, 12", "380503232032", "n.hordiienko@mail.com",
                "МК-2032", "060420252032", 159, 51, "A(II)", "Rh+");
        addPatient(2033L, "Антонюк Валерій Андрійович", LocalDate.of(1966, 9, 14), "M",
                "м. Львів, вул. Реабілітаційна, 13", "380503333033", "v.antoniuk@mail.com",
                "МК-2033", "140920252033", 176, 79, "AB(IV)", "Rh−");
        addPatient(2034L, "Кириченко Лариса Володимирівна", LocalDate.of(1978, 1, 25), "F",
                "м. Львів, вул. Реабілітаційна, 14", "380503434034", "l.kyrychenko@mail.com",
                "МК-2034", "250120252034", 162, 56, "O(I)", "Rh+");
        addPatient(2035L, "Мазуренко Віталій Денисович", LocalDate.of(1957, 6, 3), "M",
                "м. Львів, вул. Реабілітаційна, 15", "380503535035", "v.mazurenko@mail.com",
                "МК-2035", "030620252035", 173, 75, "A(II)", "Rh+");
        addPatient(2036L, "Шаповал Орися Тарасівна", LocalDate.of(1969, 11, 9), "F",
                "м. Львів, вул. Реабілітаційна, 16", "380503636036", "o.shapoval@mail.com",
                "МК-2036", "091120252036", 164, 58, "B(III)", "Rh+");
        addPatient(2037L, "Рябокінь Денис Олексійович", LocalDate.of(1974, 8, 21), "M",
                "м. Львів, вул. Реабілітаційна, 17", "380503737037", "d.ryabokin@mail.com",
                "МК-2037", "210820252037", 179, 83, "O(I)", "Rh−");
        addPatient(2038L, "Білоус Марта Іванівна", LocalDate.of(1980, 5, 17), "F",
                "м. Львів, вул. Реабілітаційна, 18", "380503838038", "m.bilous@mail.com",
                "МК-2038", "170520252038", 166, 61, "AB(IV)", "Rh+");
        addPatient(2039L, "Омельченко Роман Гнатович", LocalDate.of(1953, 3, 29), "M",
                "м. Львів, вул. Реабілітаційна, 19", "380503939039", "r.omelchenko@mail.com",
                "МК-2039", "290320252039", 171, 73, "A(II)", "Rh+");
        addPatient(2040L, "Волошина Зінаїда Борисівна", LocalDate.of(1963, 7, 4), "F",
                "м. Львів, вул. Реабілітаційна, 20", "380504040040", "z.voloshyna@mail.com",
                "МК-2040", "040720252040", 157, 50, "O(I)", "Rh+");

        for (long pid = 2001; pid <= 2020; pid++) {
            if (patients.containsKey(pid)) patients.get(pid).setDepartmentId(2L);
        }
        for (long pid = 2021; pid <= 2040; pid++) {
            if (patients.containsKey(pid)) patients.get(pid).setDepartmentId(1L);
        }
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

    void initMedicineCatalog() {
        medicineCatalog.addAll(List.of(
                new MedicineMisDTO(1L, "Paracetamol", 1, "1"),
                new MedicineMisDTO(2L, "Ibuprofen", 1, "2"),
                new MedicineMisDTO(3L, "Morphine", 14, "4"),
                new MedicineMisDTO(4L, "Fentanyl", 14, "4"),
                new MedicineMisDTO(5L, "Ceftriaxone", 2, "6"),
                new MedicineMisDTO(6L, "Metronidazole", 2, "2,3"),
                new MedicineMisDTO(7L, "Omeprazole", 3, "1"),
                new MedicineMisDTO(8L, "Heparin", 5, "5"),
                new MedicineMisDTO(9L, "Norepinephrine", 13, "3"),
                new MedicineMisDTO(10L, "Dopamine", 13, "3"),
                new MedicineMisDTO(11L, "NaCl 0.9%", 8, null),
                new MedicineMisDTO(12L, "Glucose 5%", 8, null),
                new MedicineMisDTO(13L, "Midazolam", 14, "4"),
                new MedicineMisDTO(14L, "Propofol", 14, "4"),
                new MedicineMisDTO(15L, "Dexamethasone", 1, "1"),
                new MedicineMisDTO(16L, "Insulin", 6, null),
                new MedicineMisDTO(17L, "Amlodipine", 7, "2"),
                new MedicineMisDTO(18L, "Metoclopramide", 4, "1"),
                new MedicineMisDTO(19L, "Ondansetron", 4, "2"),
                new MedicineMisDTO(20L, "Pantoprazole", 3, "1")
        ));
    }

    void initAllergies() {
        allergyData.put(1001L, List.of(
                new AllergyMisDTO(1001L, "Penicillin", 100),
                new AllergyMisDTO(1001L, "Aspirin", 101)
        ));
        allergyData.put(1002L, List.of(
                new AllergyMisDTO(1002L, "Iodine", 200)
        ));
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

    @Override
    public List<MedicineMisDTO> searchMedicineCatalog(String keyword) {
        checkErrors();
        auditService.logAction("MIS", null, "SEARCH_MEDICINE_CATALOG", getCurrentUserId());
        if (keyword == null || keyword.isBlank()) {
            return medicineCatalog;
        }
        String lower = keyword.toLowerCase();
        return medicineCatalog.stream()
                .filter(m -> m.getName().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    @Override
    public List<AllergyMisDTO> getPatientAllergies(Long patientId) {
        checkErrors();
        auditService.logAction("MIS", null, "GET_ALLERGIES", getCurrentUserId());
        return allergyData.getOrDefault(patientId, List.of());
    }
}
