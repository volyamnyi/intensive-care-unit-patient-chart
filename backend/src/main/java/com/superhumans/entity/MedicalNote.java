package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "medical_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicalNote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    private ClinicalDay clinicalDay;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "note_type", nullable = false, length = 50)
    private String noteType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;
}
