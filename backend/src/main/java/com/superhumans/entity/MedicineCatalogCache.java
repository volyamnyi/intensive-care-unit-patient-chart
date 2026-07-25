package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicine_catalog_cache")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicineCatalogCache {

    @Id
    Long id;

    @Column(nullable = false, length = 500)
    String name;

    @Column(name = "category_ref")
    Integer categoryRef;

    @Column(name = "ptg_code", length = 50)
    String ptgCode;

    @Column(name = "is_high_risk")
    Boolean isHighRisk;

    @Column(name = "cached_at", nullable = false)
    LocalDateTime cachedAt;

    @PrePersist
    void prePersist() {
        if (cachedAt == null) {
            cachedAt = LocalDateTime.now();
        }
        if (isHighRisk == null) {
            isHighRisk = categoryRef != null && (categoryRef == 13 || categoryRef == 14);
        }
    }
}
