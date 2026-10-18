package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * #363: the editingUserId/editingStartedAt fields were removed from the entity.
 * The columns stay in the database (Liquibase history is never rewritten), but
 * nothing read them: the lock built on them had no caller in src/main, and
 * acquireLock stored the user id in the STATUS column. Concurrent editing is
 * guarded by the {@code @Version} optimistic lock inherited from BaseEntity -
 * a lost update between two editors surfaces as HTTP 409 VERSION_CONFLICT.
 */
@Entity
@Table(name = "prescription_lists")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionList extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    Long patientId;

    @Column(name = "hospitalization_id")
    UUID hospitalizationId;

    @Column(name = "department_id")
    Long departmentId;

    @Column(name = "document_name")
    String documentName;

    @Column(name = "status", nullable = false, length = 32)
    String status;

    public boolean isFinished() {
        return "Finished".equals(status);
    }
}
