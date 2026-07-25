package com.superhumans.repository;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.AppException;
import com.superhumans.model.medicinelist.*;
import com.superhumans.model.patient.IdsAndDateTimes;
import com.superhumans.model.patient.Patient;
import com.superhumans.model.user.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicineListRepository {

    JdbcTemplate jdbcTemplate;

    ObjectMapper objectMapper;

    public void createNewMedicineList(MedicineList medicineList, String json, String vlJson, String approvedRowIndexes) {
        String sql = "INSERT INTO MedicineList (PatientRef, MedicineListCreationUser, MedicineListCreationDate, DocumentName, ApprovedRowIndexes) VALUES (?,?,?,?,?);";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        String finalSql = sql;
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(finalSql, Statement.RETURN_GENERATED_KEYS);
                    ps.setInt(1, medicineList.getPatientRef());
                    ps.setString(2, medicineList.getMedicineListCreationUser());
                    ps.setString(3, String.valueOf(medicineList.getMedicineListCreationDate()));
                    ps.setString(4, medicineList.getDocumentName());
                    ps.setString(5, approvedRowIndexes);
                    return ps;
                },
                keyHolder
        );

        Integer medicineListId = ((BigDecimal) keyHolder.getKeyList().get(keyHolder.getKeyList().size() - 1).get("GENERATED_KEYS")).intValue();


        sql = "INSERT INTO MedicineListItem(MedicineListRef, MedicineListItemEditUser, MedicineListItemEditDate, MedicineDetails, VitalList, Status) VALUES (?,?,?,?,?,?);";
        jdbcTemplate.update(
                sql,
                medicineListId,
                medicineList.getMedicineListCreationUser(),
                //String.valueOf(medicineList.getMedicineListCreationDate()),
                LocalDateTime.now(),
                json,
                vlJson,
                "Saved"
        );
    }

    public List<MedicineList> getAllMedicineLists() {
        RowMapper<MedicineList> mapper = new RowMapper() {

            @Override
            @SneakyThrows
            public MedicineList mapRow(ResultSet rs, int rowNum) {
                MedicineList medicalList = new MedicineList();
                medicalList.setMedicineListID(rs.getObject("MedicineListID", Integer.class));
                medicalList.setPatientRef(rs.getObject("PatientRef", Integer.class));
                medicalList.setDocumentName(rs.getObject("DocumentName", String.class));
                medicalList.setMedicineListCreationUser(rs.getObject("MedicineListCreationUser", String.class));
                medicalList.setMedicineListCreationDate(rs.getObject("MedicineListCreationDate", LocalDateTime.class));
                medicalList.setStatus(rs.getObject("Status", String.class));
                return medicalList;
            }
        };
        return jdbcTemplate.query("SELECT MedicineListId, PatientRef, DocumentName, MedicineListCreationUser, MedicineListCreationDate, mli.Status FROM MedicineList ml Left Join MedicineListItem mli ON ml.MedicineListID=mli.MedicineListRef", mapper);
    }

    public MedicineList getMedicineListById(Integer id) {
        RowMapper<MedicineList> mapper = new RowMapper() {

            @SneakyThrows
            @Override
            public MedicineList mapRow(ResultSet rs, int rowNum) {
                MedicineList medicalList = new MedicineList();
                medicalList.setMedicineListID(rs.getObject("MedicineListID", Integer.class));
                medicalList.setPatientRef(rs.getObject("PatientRef", Integer.class));
                medicalList.setDocumentName(rs.getObject("DocumentName", String.class));
                medicalList.setMedicineListCreationUser(rs.getObject("MedicineListCreationUser", String.class));
                medicalList.setStatus(rs.getObject("Status", String.class));
                medicalList.setMedicineListCreationDate(rs.getObject("MedicineListCreationDate", LocalDateTime.class));

                List<MedicineDetails> medicineDetails = objectMapper.readValue(rs.getString("MedicineDetails"), new TypeReference<>() {

                });


                VitalList vitalList = Optional.ofNullable(rs.getString("VitalList"))
                        .filter(s -> !s.isBlank())
                        .map(s -> {
                            try {
                                return objectMapper.readValue(s, new TypeReference<VitalList>() {});
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .orElse(null);

                String approvedJson = rs.getString("ApprovedRowIndexes");


                if (approvedJson == null || approvedJson.isBlank()) {
                    approvedJson = "[]";
                }

                TreeSet<String> approvedRowIndexes = objectMapper.readValue(
                        approvedJson,
                        new TypeReference<>() {
                        }
                );
                medicalList.setMedicineDetails(medicineDetails);
                medicalList.setVitalList(vitalList);
                medicalList.setApprovedRowIndexes(approvedRowIndexes);

                return medicalList;
            }
        };

        List<MedicineList> result = jdbcTemplate.query("SELECT * FROM MedicineList ml LEFT JOIN MedicineListItem mli ON ml.MedicineListID = mli.MedicineListRef WHERE ml.MedicineListID = ?;", mapper, id);

        return result.get(0);
    }

    public void updateMedicineListById(MedicineList medicineList, String json, String vlJson ,String approvedRowIndexes) {
        RowMapper<MedicineDetails> mapper = new RowMapper() {

            @SneakyThrows
            @Override
            public MedicineDetails mapRow(ResultSet rs, int rowNum) {
                MedicineDetails medicalListItem = new MedicineDetails();
                medicalListItem.setStatus(rs.getObject("Status", String.class));
                return medicalListItem;
            }
        };

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        List<MedicineDetails> result = jdbcTemplate.query("SELECT Status FROM MedicineListItem WHERE MedicineListRef = ?;", mapper, medicineList.getMedicineListID());
        if (result.get(0).getStatus().equals(getCurrentLogin()) || result.get(0).getStatus().equals("Saved")) {

            if (user.getBusinessRole().equals("DOCTOR")) {
                String sql = "UPDATE MedicineListItem SET MedicineListItemEditUser = ?, MedicineListItemEditDate = ?, MedicineDetails = ?, VitalList = ?, Status = ? WHERE MedicineListRef = ?; UPDATE MedicineList SET ApprovedRowIndexes = ? WHERE MedicineListID = ?;";
                jdbcTemplate.update(
                        sql,
                        medicineList.getMedicineListCreationUser(),
                        //String.valueOf(medicineList.getMedicineListCreationDate()),
                        LocalDateTime.now(),
                        json,
                        vlJson,
                        getCurrentLogin(),
                        medicineList.getMedicineListID(),
                        approvedRowIndexes,
                        medicineList.getMedicineListID()
                );
            } else {
                String sql = "UPDATE MedicineListItem SET MedicineListItemEditUser = ?, MedicineDetails = ?, VitalList = ?, Status = ? WHERE MedicineListRef = ?;";
                jdbcTemplate.update(
                        sql,
                        medicineList.getMedicineListCreationUser(),
                        //String.valueOf(medicineList.getMedicineListCreationDate()),
                        json,
                        vlJson,
                        getCurrentLogin(),
                        medicineList.getMedicineListID()
                );
            }
        } else {
            throw new AppException("Документ зараз редагується користувачем " + result.get(0).getStatus(), HttpStatus.CONFLICT);
        }
    }

    public Boolean isDocumentEditing(Integer id) {
        RowMapper<MedicineDetails> mapper = new RowMapper() {

            @SneakyThrows
            @Override
            public MedicineDetails mapRow(ResultSet rs, int rowNum) {
                MedicineDetails medicalListItem = new MedicineDetails();
                medicalListItem.setStatus(rs.getObject("Status", String.class));
                return medicalListItem;
            }
        };

        List<MedicineDetails> result = jdbcTemplate.query("SELECT Status FROM MedicineListItem WHERE MedicineListRef = ?;", mapper, id);
        if (!result.get(0).getStatus().equals("Saved")) {
            if (!result.get(0).getStatus().equals(getCurrentLogin())) {
                if(result.get(0).getStatus().equals("Finished")) {
                    throw new AppException("Даний листок призначень закрито", HttpStatus.CONFLICT);
                }
                throw new AppException("Документ зараз редагується користувачем " + result.get(0).getStatus(), HttpStatus.CONFLICT);
            }
        }

        return false;
    }

    public void updateMedicineListStatusByListId(Integer id, String status) {
        //System.out.println("Updating status by id..." + status);
        RowMapper<MedicineDetails> mapper = new RowMapper() {

            @SneakyThrows
            @Override
            public MedicineDetails mapRow(ResultSet rs, int rowNum) {
                MedicineDetails medicalListItem = new MedicineDetails();
                medicalListItem.setStatus(rs.getObject("Status", String.class));
                return medicalListItem;
            }
        };

        List<MedicineDetails> result = jdbcTemplate.query("SELECT Status FROM MedicineListItem WHERE MedicineListRef = ?;", mapper, id);
        if (result.get(0).getStatus().equals("Saved") || result.get(0).getStatus().equals(getCurrentLogin())) {
            LocalDateTime now = LocalDateTime.now();
            String sql = "UPDATE MedicineListItem SET Status = ?, MedicineListItemLastVisitTime = ? WHERE MedicineListRef = ?;";
            jdbcTemplate.update(
                    sql,
                    status,
                    now,
                    id
            );
        }

    }

    public static String getUsernameFromJwt(String token) {
        DecodedJWT decodedJWT = JWT.decode(token);
        return decodedJWT.getSubject();
    }

    public static String getCurrentLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            return user.getLogin();
        }
        return null;
    }

    public List<Patient> searchPatients(String keyword) {
        RowMapper<Patient> mapper = new RowMapper() {

            @Override
            @SneakyThrows
            public Patient mapRow(ResultSet rs, int rowNum) {
                Patient patient = new Patient();
                patient.setId(rs.getObject("PatientID", Integer.class));
                patient.setName(rs.getObject("PatientName", String.class));
                return patient;
            }
        };

        String sql = "SELECT PatientID, PatientName FROM Patient WHERE PatientName LIKE ?";
        String query = "%" + keyword + "%";
        return jdbcTemplate.query(sql, mapper, query);
    }

    public List<Patient> getAllInpatients(String order, String residence) {

        RowMapper<Patient> mapper = new RowMapper() {

            @Override
            @SneakyThrows
            public Patient mapRow(ResultSet rs, int rowNum) {
                Patient patient = new Patient();
                patient.setId(rs.getObject("PatientID", Integer.class));
                patient.setName(rs.getObject("PatientName", String.class));
                patient.setHistoryNumber(rs.getObject("PatientHistoryNumber", String.class));
                patient.setAddress(rs.getObject("PatientAddress", String.class));
                patient.setPhone(rs.getObject("PatientPhone", String.class));
                patient.setDepartment(rs.getObject("VenueLevel2", String.class));
                patient.setRoomNumber(rs.getObject("VenueLevel1", String.class));
                patient.setBedNumber(rs.getObject("VenueLevel0", String.class));
                patient.setDoctor(rs.getObject("Doctor", String.class));
                patient.setDoctorUserName(rs.getObject("DoctorUserName", String.class));
                patient.setGender(rs.getObject("PatientSexRef", String.class));
                patient.setAge(rs.getObject("Age", String.class));
                patient.setBirthDate(rs.getObject("PatientBirthDate", String.class));
                patient.setResidenceStatus(rs.getObject("ResidenceStatusRef", String.class));

                return patient;
            }
        };

        String orderBy = "";
        String ascDesc = order.split(":")[1];

        if(order.split(":")[0].equals("byName")) {
            orderBy = "PatientName";
        } else if(order.split(":")[0].equals("byStatus")) {
            orderBy = "ResidenceStatusRef";
        } else if(order.split(":")[0].equals("byDoctor")) {
            orderBy = "Doctor";
        } else if(order.split(":")[0].equals("byId")) {
            orderBy = "PatientID";
        } else if(order.split(":")[0].equals("byRoom")) {
            orderBy = "VenueLevel1";
        }
        else if(order.split(":")[0].equals("byBed")) {
            orderBy = "VenueLevel0";
        }



        List<Patient> patients = jdbcTemplate.query(
                "Select PatientID, PatientName," +
                        "PatientHistoryNumber," +
                        "PatientAddress," +
                        "PatientPhone," +
                        "v2.VenueName AS VenueLevel2," +
                        "v1.VenueName AS VenueLevel1," +
                        "v.VenueName AS VenueLevel0," +
                        "PatientSexRef," +
                        "DATEDIFF(YEAR, PatientBirthDate, GETDATE()) as Age, PatientBirthDate, u.UserName AS Doctor, u.UserLogin as DoctorUserName, ResidenceStatusRef from Patient " +
                        "left join Residence r on PatientRef = PatientID " + //and ResidenceStatusRef = N'PRG'
                        "left join venue v on r.VenueRef = v.VenueID " +
                        "left join venue v1 on v.VenueParentRef = v1.VenueID " +
                        "left join venue v2 on v1.VenueParentRef = v2.VenueID " +
                        "left join Users u on u.UserLogin = r.UserRef " +
                        "where ResidenceSequence1Ref in (?) AND isNull(ResidenceEndDate,getDate()) BETWEEN getDate()-14 AND getDate()+1 ORDER BY "+ orderBy +" " + ascDesc + ";", mapper, residence);


        String editDatesSql = "SELECT mli.MedicineListItemEditDate, ml.PatientRef " +
                "FROM MedicineListItem mli " +
                "LEFT JOIN MedicineList ml ON mli.MedicineListRef = ml.MedicineListID";

        RowMapper<IdsAndDateTimes> dateTimesMapper = new RowMapper<IdsAndDateTimes>() {
            @Override
            public IdsAndDateTimes mapRow(ResultSet rs, int rowNum) throws SQLException {
                IdsAndDateTimes idsAndDateTimes = new IdsAndDateTimes();
                idsAndDateTimes.setPatientId(rs.getObject("PatientRef", Integer.class));
                idsAndDateTimes.setEditDateTime(rs.getObject("MedicineListItemEditDate", LocalDateTime.class));

                return idsAndDateTimes;
            }
        };

        List<IdsAndDateTimes> idDateTimes = jdbcTemplate.query(editDatesSql, dateTimesMapper);

        for (int i = 0; i < patients.size(); i++) {
            for (int j = 0; j < idDateTimes.size(); j++) {
                if (patients.get(i).getId().equals(idDateTimes.get(j).getPatientId())) {
                    patients.get(i).getMedicineListEditDates().add(idDateTimes.get(j).getEditDateTime());
                }
            }
        }

        return patients;
    }


    public List<Patient> getAllRecentlyInpatients() {

        RowMapper<Patient> mapper = new RowMapper() {

            @Override
            @SneakyThrows
            public Patient mapRow(ResultSet rs, int rowNum) {
                Patient patient = new Patient();
                patient.setId(rs.getObject("PatientID", Integer.class));
                patient.setName(rs.getObject("PatientName", String.class));
                patient.setHistoryNumber(rs.getObject("PatientHistoryNumber", String.class));
                patient.setAddress(rs.getObject("PatientAddress", String.class));
                patient.setPhone(rs.getObject("PatientPhone", String.class));
                patient.setDepartment(rs.getObject("VenueLevel2", String.class));
                patient.setRoomNumber(rs.getObject("VenueLevel1", String.class));
                patient.setBedNumber(rs.getObject("VenueLevel0", String.class));
                patient.setDoctor(rs.getObject("Doctor", String.class));
                patient.setDoctorUserName(rs.getObject("DoctorUserName", String.class));
                patient.setGender(rs.getObject("PatientSexRef", String.class));
                patient.setAge(rs.getObject("Age", String.class));
                patient.setBirthDate(rs.getObject("PatientBirthDate", String.class));
                patient.setResidenceStatus(rs.getObject("ResidenceStatusRef", String.class));

                return patient;
            }
        };


        List<Patient> patients = jdbcTemplate.query(
                "Select PatientID, PatientName," +
                        "PatientHistoryNumber," +
                        "PatientAddress," +
                        "PatientPhone," +
                        "v2.VenueName AS VenueLevel2," +
                        "v1.VenueName AS VenueLevel1," +
                        "v.VenueName AS VenueLevel0," +
                        "PatientSexRef," +
                        "DATEDIFF(YEAR, PatientBirthDate, GETDATE()) as Age, PatientBirthDate, u.UserName AS Doctor, u.UserLogin as DoctorUserName, ResidenceStatusRef from Patient " +
                        "left join Residence r on PatientRef = PatientID and ResidenceStatusRef = N'PRG' " +
                        "left join venue v on r.VenueRef = v.VenueID " +
                        "left join venue v1 on v.VenueParentRef = v1.VenueID " +
                        "left join venue v2 on v1.VenueParentRef = v2.VenueID " +
                        "left join Users u on u.UserLogin = r.UserRef " +
                        "where ResidenceSequence1Ref in (19, 37) and datediff(hour, ResidenceStartDate, getdate())<=24 and patientID not in (select patientref from MedicineList m where datediff(hour, m.MedicineListCreationDate, getdate())<24) ORDER BY PatientName ASC;", mapper);


        String editDatesSql = "SELECT mli.MedicineListItemEditDate, ml.PatientRef " +
                "FROM MedicineListItem mli " +
                "LEFT JOIN MedicineList ml ON mli.MedicineListRef = ml.MedicineListID";

        RowMapper<IdsAndDateTimes> dateTimesMapper = new RowMapper<IdsAndDateTimes>() {
            @Override
            public IdsAndDateTimes mapRow(ResultSet rs, int rowNum) throws SQLException {
                IdsAndDateTimes idsAndDateTimes = new IdsAndDateTimes();
                idsAndDateTimes.setPatientId(rs.getObject("PatientRef", Integer.class));
                idsAndDateTimes.setEditDateTime(rs.getObject("MedicineListItemEditDate", LocalDateTime.class));

                return idsAndDateTimes;
            }
        };

        List<IdsAndDateTimes> idDateTimes = jdbcTemplate.query(editDatesSql, dateTimesMapper);

        for (int i = 0; i < patients.size(); i++) {
            for (int j = 0; j < idDateTimes.size(); j++) {
                if (patients.get(i).getId().equals(idDateTimes.get(j).getPatientId())) {
                    patients.get(i).getMedicineListEditDates().add(idDateTimes.get(j).getEditDateTime());
                }
            }
        }

        return patients;
    }

    public Patient getPatientById(Integer id) {
        RowMapper<Patient> mapper = new RowMapper() {

            @SneakyThrows
            @Override
            public Patient mapRow(ResultSet rs, int rowNum) {
                Patient patient = new Patient();
                patient.setId(rs.getObject("PatientID", Integer.class));
                patient.setName(rs.getObject("PatientName", String.class));
                patient.setHistoryNumber(rs.getObject("PatientHistoryNumber", String.class));
                patient.setAddress(rs.getObject("PatientAddress", String.class));
                patient.setPhone(rs.getObject("PatientPhone", String.class));
                patient.setDepartment(rs.getObject("VenueLevel2", String.class));
                patient.setRoomNumber(rs.getObject("VenueLevel1", String.class));
                patient.setBedNumber(rs.getObject("VenueLevel0", String.class));
                patient.setDoctor(rs.getObject("Doctor", String.class));
                patient.setDoctorUserName(rs.getObject("DoctorUserName", String.class));
                patient.setGender(rs.getObject("PatientSexRef", String.class));
                patient.setAge(rs.getObject("Age", String.class));
                patient.setBirthDate(rs.getObject("PatientBirthDate", String.class));

                return patient;
            }
        };

        List<Patient> result = jdbcTemplate.query(
                "Select PatientID, PatientName," +
                        "PatientHistoryNumber," +
                        "PatientAddress," +
                        "PatientPhone," +
                        "v2.VenueName AS VenueLevel2," +
                        "v1.VenueName AS VenueLevel1," +
                        "v.VenueName AS VenueLevel0," +
                        "PatientSexRef," +
                        "DATEDIFF(YEAR, PatientBirthDate, GETDATE()) as Age, PatientBirthDate, u.UserName AS Doctor, u.UserLogin as DoctorUserName from Patient " +
                        "left join Residence r on PatientRef = PatientID and ResidenceStatusRef = N'PRG'" +
                        "left join venue v on r.VenueRef = v.VenueID " +
                        "left join venue v1 on v.VenueParentRef = v1.VenueID " +
                        "left join venue v2 on v1.VenueParentRef = v2.VenueID " +
                        "left join Users u on u.UserLogin = r.UserRef " +
                        "where PatientID = ?;", mapper, id);

        return result.get(0);
    }

    public List<MedicineList> getAllDocumentsByPatientId(Integer id) {
        RowMapper<MedicineList> mapper = new RowMapper() {

            @Override
            @SneakyThrows
            public MedicineList mapRow(ResultSet rs, int rowNum) {
                MedicineList medicalList = new MedicineList();
                medicalList.setMedicineListID(rs.getObject("MedicineListID", Integer.class));
                medicalList.setPatientRef(rs.getObject("PatientRef", Integer.class));
                medicalList.setDocumentName(rs.getObject("DocumentName", String.class));
                medicalList.setMedicineListCreationUser(rs.getObject("MedicineListCreationUser", String.class));
                medicalList.setMedicineListCreationDate(rs.getObject("MedicineListCreationDate", LocalDateTime.class));
                medicalList.setStatus(rs.getObject("Status", String.class));
                return medicalList;
            }
        };

        return jdbcTemplate.query("SELECT MedicineListId, PatientRef, DocumentName, MedicineListCreationUser, MedicineListCreationDate, mli.Status FROM MedicineList ml Left Join MedicineListItem mli On MedicineListID=MedicineListRef WHERE PatientRef = ?", mapper, id);
    }

    public List<Medicine> searchMedicine(String keyword) {
        RowMapper<Medicine> mapper = new RowMapper() {

            @Override
            @SneakyThrows
            public Medicine mapRow(ResultSet rs, int rowNum) {
                Medicine medicine = new Medicine();
                medicine.setId(rs.getObject("ItemKindID", Integer.class));
                medicine.setName(rs.getObject("ItemKindName", String.class));
                medicine.setCategoryRef(rs.getObject("ItemKindMedicineCategoryRef", Integer.class));
                medicine.setPTG(rs.getObject("ItemKindPTG", String.class));
                return medicine;
            }
        };

        String sql = "SELECT DISTINCT ik.ItemKindID, ik.ItemKindName, ik.ItemKindMedicineCategoryRef, ik.ItemKindPTG FROM ItemKind ik LEFT JOIN item i ON ik.itemkindid = i.itemkindref WHERE i.ItemStartDate >= '2025-01-01' AND i.itemleftunitquantity > 0 AND LTRIM(RTRIM(ik.ItemKindName)) COLLATE Latin1_General_CI_AS LIKE ?";
        String query = "%" + keyword + "%";

        return jdbcTemplate.query(sql, mapper, query);
    }

    public void deleteMedicineListById(Integer id) {
        String sql = """
                    DELETE FROM MedicineListItem WHERE MedicineListRef = ?;
                    DELETE FROM MedicineList WHERE MedicineListID = ?;
                """;

        jdbcTemplate.update(sql, id, id);
    }

    public void addNewChatId(Long chatId) {
        String sql = "INSERT INTO SH_MedicineListBotChatIds (chat_id) VALUES (?);";

        String finalSql = sql;
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(finalSql);
                    ps.setLong(1, chatId);
                    return ps;
                }
        );
    }

    public List<Long> getAllChatIds() {
        String sql = "SELECT chat_id from SH_MedicineListBotChatIds";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("chat_id"));
    }

    public void generateDeDocument(Integer medicineListID, String documentDateTime) {
        String sql = "UPDATE MedicineList SET MakeDEDocument = ? WHERE MedicineListID = ?;";
        jdbcTemplate.update(
                sql,
                documentDateTime,
                medicineListID
        );
    }


    public List<Allergies> getAllAllergiesByPatientId(Integer id) {
        RowMapper<Allergies> mapper = new RowMapper() {

            @Override
            @SneakyThrows
            public Allergies mapRow(ResultSet rs, int rowNum) {
                Allergies allergies = new Allergies();
                allergies.setDocumentId(rs.getObject("DocumentID", Integer.class));
                allergies.setAllergiesList(rs.getObject("DocumentNodeXmlValue", String.class));

                return allergies;
            }
        };

        return jdbcTemplate.query("SELECT DocumentID, DocumentNodeXmlValue FROM Document " +
                "Left Join course on CourseRef = CourseId " +
                "Left Join DocumentNode on DocumentRef=documentId and DocumentNodeStaticGuidRef in('17e19e73-a280-4f53-980a-2fc43fbea432','fd2d5a72-0b94-42cd-9b88-c6dd9a251389') " +
                "Where PatientRef = ? and DocumentTemplateRef in (23, 166);", mapper, id);
    }

    public List<Medicine> getHighRiskMedicineByName(String highRiskMedicineName) {
        RowMapper<Medicine> mapper = new RowMapper() {

            @Override
            @SneakyThrows
            public Medicine mapRow(ResultSet rs, int rowNum) {
                Medicine medicine = new Medicine();
                medicine.setId(rs.getObject("ItemKindID", Integer.class));
                medicine.setName(rs.getObject("ItemKindName", String.class));
                medicine.setCategoryRef(rs.getObject("ItemKindMedicineCategoryRef", Integer.class));
                return medicine;
            }
        };

        String sql = "SELECT ItemKindID, ItemKindName, ItemKindMedicineCategoryRef FROM ItemKind WHERE ItemKindName = ? AND (ItemKindMedicineCategoryRef = 14 OR ItemKindMedicineCategoryRef = 13)";
        String query = highRiskMedicineName;

        return jdbcTemplate.query(sql, mapper, query);
    }

    public List<Medicine> getConflictMedicineByName(String conflictMedicineName) {
        RowMapper<Medicine> mapper = new RowMapper() {

            @Override
            @SneakyThrows
            public Medicine mapRow(ResultSet rs, int rowNum) {
                Medicine medicine = new Medicine();
                medicine.setId(rs.getObject("ItemKindID", Integer.class));
                medicine.setName(rs.getObject("ItemKindName", String.class));
                medicine.setCategoryRef(rs.getObject("ItemKindMedicineCategoryRef", Integer.class));
                medicine.setPTG(rs.getObject("ItemKindPTG", String.class));
                return medicine;
            }
        };

        String sql = "select ItemKindId, ItemKindName, ItemKindMedicineCategoryRef, ItemKindPTG from ItemKind where ItemKindName = ? and ItemKindPTG in ('1','2','3','4','5','6','2,3')";
        String query = conflictMedicineName;

        return jdbcTemplate.query(sql, mapper, query);
    }

    public void closeMedicineListByListId(Integer id) {
        String sql = "Update MedicineListItem set Status='Finished' Where MedicineListRef=?;";
        jdbcTemplate.update(
                sql,
                id
        );
    }
}
