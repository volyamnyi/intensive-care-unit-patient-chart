package com.superhumans.entity.core;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import com.superhumans.entity.base.BaseEntity;

@Entity
@Table(name = "reference_values", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"type", "code"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferenceValue extends BaseEntity {

    @Column(nullable = false, length = 100)
    String type;

    @Column(nullable = false, length = 100)
    String code;

    @Column(nullable = false, length = 500)
    String value;

    @Column(name = "sort_order")
    Integer sortOrder;

    @Column(name = "is_active")
    @Builder.Default
    Boolean isActive = true;
}
