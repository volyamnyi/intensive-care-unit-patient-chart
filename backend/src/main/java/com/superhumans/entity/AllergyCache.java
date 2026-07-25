package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "allergy_cache", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"patient_id", "allergen_name"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AllergyCache {

    @Id
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    UUID id;

    @Column(name = "patient_id", nullable = false)
    Long patientId;

    @Column(name = "allergen_name", nullable = false, length = 500)
    String allergenName;

    @Column(name = "source_document_id")
    Integer sourceDocumentId;

    @Column(name = "cached_at", nullable = false)
    LocalDateTime cachedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (cachedAt == null) {
            cachedAt = LocalDateTime.now();
        }
    }
}
