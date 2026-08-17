package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(name = "editing_user_id")
    UUID editingUserId;

    @Column(name = "editing_started_at")
    LocalDateTime editingStartedAt;

    public boolean isEditing() {
        return editingUserId != null && !"Finished".equals(status) && !"Saved".equals(status);
    }

    public boolean isFinished() {
        return "Finished".equals(status);
    }
}
