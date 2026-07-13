package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clinical_scales")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClinicalScale extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_automatic")
    private Boolean isAutomatic;

    @Column(nullable = false, length = 20)
    private String status;
}
