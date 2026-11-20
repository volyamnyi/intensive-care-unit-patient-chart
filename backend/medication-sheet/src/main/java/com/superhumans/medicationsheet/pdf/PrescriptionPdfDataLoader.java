package com.superhumans.medicationsheet.pdf;

import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfSnapshot.Execution;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfSnapshot.Order;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfSnapshot.Part;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfSnapshot.Row;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.repository.core.SystemSettingsRepository;
import com.superhumans.repository.core.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PrescriptionPdfDataLoader {
    // One PostgreSQL statement = one MVCC snapshot, including the anchor of the empty day scaffolding.
    // Count sibling parts BEFORE joining events: keep even a blank contributor to a key collision.
    private static final String CLINICAL = """
            WITH days AS (
                SELECT d.id, d.day_date, d.item_id
                FROM prescription_item_days d JOIN prescription_items i ON i.id = d.item_id
                WHERE i.list_id = ? AND i.is_deleted IS NOT TRUE AND d.is_deleted IS NOT TRUE
            ), parts AS (
                SELECT d.item_id, d.day_date, p.*,
                       count(*) OVER (PARTITION BY d.item_id, d.day_date, lower(trim(p.period))) AS siblings
                FROM days d JOIN prescription_day_parts p ON p.day_id = d.id AND p.is_deleted IS NOT TRUE
            ), facts AS (
                SELECT p.*, e.id AS event_id, e.day_part_id AS event_part_id, e.status AS event_status,
                       e.actual_dose, e.executed_by, e.executed_at, e.administered_at, e.dispensed_at,
                       e.administered_at_manual, e.comment
                FROM parts p LEFT JOIN prescription_executions e ON e.day_part_id = p.id AND e.is_deleted IS NOT TRUE
                WHERE p.siblings > 1 OR p.is_planned OR p.is_planned_finished OR p.is_completed OR p.is_completed_finished
                   OR e.id IS NOT NULL OR nullif(trim(p.dose), '') IS NOT NULL
                   OR nullif(trim(p.doctor_name), '') IS NOT NULL OR nullif(trim(p.nurse_name), '') IS NOT NULL
                   OR coalesce(lower(trim(p.period)), '') NOT IN ('morning','day','evening','night')
            )
            SELECT l.patient_id, (SELECT min(day_date) FROM days) AS first_day,
                   i.medicine_name, i.medicine_method, i.regime, i.sort_order, f.*
            FROM prescription_lists l LEFT JOIN facts f ON l.patient_id IS NOT NULL LEFT JOIN prescription_items i ON i.id = f.item_id
            WHERE l.id = ? AND l.is_deleted IS NOT TRUE
            """;
    private static final List<String> SETTINGS = List.of("institution_name", "institution_edrpou",
            "institution_authority", "institution_address", "prescription_print_profile");
    private final EntityManager med;
    private final TransactionTemplate medRead;
    private final TransactionTemplate coreRead;
    private final SystemSettingsRepository settings;
    private final UserRepository users;
    private final MisService mis;

    public PrescriptionPdfDataLoader(@Qualifier("medEntityManagerFactory") EntityManagerFactory emf,
            @Qualifier("medTransactionManager") PlatformTransactionManager medTx,
            @Qualifier("coreTransactionManager") PlatformTransactionManager coreTx,
            SystemSettingsRepository settings, UserRepository users, MisService mis) {
        med = SharedEntityManagerCreator.createSharedEntityManager(emf);
        medRead = readOnly(medTx);
        coreRead = readOnly(coreTx);
        this.settings = settings;
        this.users = users;
        this.mis = mis;
    }

    private static TransactionTemplate readOnly(PlatformTransactionManager tx) {
        TransactionTemplate result = new TransactionTemplate(tx);
        result.setReadOnly(true);
        return result;
    }

    public record LoadedSnapshot(PrescriptionPdfSnapshot snapshot, PrescriptionPdfPagePlan.Header header,
                                 Map<UUID, String> logins) { }
    private record Clinical(long patientId, PrescriptionPdfSnapshot snapshot) { }
    private record Core(Map<String, String> settings, Map<UUID, String> logins) { }

    public LoadedSnapshot load(UUID listId) {
        Clinical clinical = medRead.execute(tx -> med.unwrap(Session.class).doReturningWork(connection -> {
            try (var query = connection.prepareStatement(CLINICAL)) {
                query.setObject(1, listId);
                query.setObject(2, listId);
                query.setFetchSize(512);
                try (var rows = query.executeQuery()) {
                    return read(rows, listId);
                }
            }
        }));
        Core core = coreRead.execute(tx -> {
            Map<String, String> values = new HashMap<>();
            settings.findByKeyIn(SETTINGS).forEach(setting -> values.put(setting.getKey(), setting.getValue()));
            return new Core(values, PrescriptionPdfUsernameResolver.index(users.findAllLogins()));
        });
        // No database transaction spans the external lookup or rendering (#383).
        Optional<PatientDTO> patient = mis.getPatient(clinical.patientId());
        Map<String, String> s = core.settings();
        var header = new PrescriptionPdfPagePlan.Header(s.get("institution_name"), s.get("institution_edrpou"),
                s.get("institution_authority"), s.get("institution_address"), "superhumans".equals(s.get("prescription_print_profile")),
                patient.map(PatientDTO::getFullName).orElse(""), patient.map(PatientDTO::getRoom).orElse(""));
        return new LoadedSnapshot(clinical.snapshot(), header, core.logins());
    }

    private Clinical read(ResultSet rs, UUID id) throws SQLException {
        if (!rs.next()) {
            throw new NotFoundException("Prescription list not found: " + id);
        }
        long patient = rs.getLong("patient_id");
        LocalDate anchor = rs.getObject("first_day", LocalDate.class);
        List<Row> rows = new ArrayList<>();
        do {
            UUID partId = rs.getObject("id", UUID.class);
            if (partId == null) {
                continue; // Existing list without printable facts.
            }
            Order order = new Order(rs.getObject("item_id", UUID.class), rs.getInt("sort_order"),
                    rs.getString("medicine_name"), rs.getString("medicine_method"), rs.getString("regime"));
            Part part = new Part(partId, rs.getObject("day_id", UUID.class), rs.getString("period"), rs.getString("dose"),
                    rs.getBoolean("is_planned"), rs.getBoolean("is_planned_finished"), rs.getBoolean("is_completed"),
                    rs.getBoolean("is_completed_finished"), PrescriptionPdfSnapshot.present(rs.getString("doctor_name"))
                    || PrescriptionPdfSnapshot.present(rs.getString("nurse_name")));
            UUID event = rs.getObject("event_id", UUID.class);
            rows.add(new Row(order, rs.getObject("day_date", LocalDate.class), part, event == null ? null
                    : new Execution(event, rs.getObject("event_part_id", UUID.class), rs.getObject("executed_by", UUID.class),
                    rs.getString("event_status"), rs.getString("actual_dose"), rs.getObject("administered_at", LocalDateTime.class),
                    rs.getObject("dispensed_at", LocalDateTime.class), rs.getBoolean("administered_at_manual"),
                    rs.getObject("executed_at", LocalDateTime.class), rs.getString("comment"))));
        } while (rs.next());
        return new Clinical(patient, new PrescriptionPdfSnapshot(anchor, rows));
    }
}
