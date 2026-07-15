package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reference_values", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"type", "code"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReferenceValue extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String type;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 500)
    private String value;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
