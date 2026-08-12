package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "clinical_scales")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClinicalScale extends BaseEntity {

    @Column(nullable = false, length = 200)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "is_automatic")
    Boolean isAutomatic;

    @Column(nullable = false, length = 20)
    String status;
}
