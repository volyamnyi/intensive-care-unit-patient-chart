package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import com.superhumans.entity.base.BaseEntity;

@Entity
@Table(name = "medical_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalNote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    ClinicalDay clinicalDay;

    @Column(name = "author_id", nullable = false)
    Long authorId;

    @Column(nullable = false, length = 20)
    String role;

    @Column(name = "note_type", nullable = false, length = 50)
    String noteType;

    @Column(nullable = false, columnDefinition = "TEXT")
    String text;
}
