package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "prosthetics_failure_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FailureSnapshot extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false, unique = true)
    FlowInstance instance;

    @Column(name = "category", nullable = false, length = 64)
    String category;

    @Column(name = "description")
    String description;

    @Column(name = "snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    String snapshot;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Failure category is required");
        }
    }
}
