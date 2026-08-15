package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.base.BaseEntity;

import com.superhumans.prosthetismanufacturing.entity.ElementType;
import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;

@Entity
@Table(name = "prosthetics_template_elements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateElement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    TemplateStep step;

    @Column(name = "order_index", nullable = false)
    Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "element_type", nullable = false, length = 32)
    ElementType elementType;

    @Column(name = "label", nullable = false)
    String label;

    @Column(name = "placeholder")
    String placeholder;

    @Column(name = "required")
    @Builder.Default
    Boolean required = false;

    @Column(name = "unit", length = 32)
    String unit;

    @Column(name = "min_value", precision = 12, scale = 3)
    BigDecimal minValue;

    @Column(name = "max_value", precision = 12, scale = 3)
    BigDecimal maxValue;

    @Column(name = "min_count")
    Integer minCount;

    @Column(name = "max_count")
    Integer maxCount;

    @Column(name = "mime_types")
    String mimeTypes;

    @Column(name = "max_size_mb")
    Integer maxSizeMb;

    @Column(name = "regex_pattern")
    String regexPattern;

    @Column(name = "options", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    String options;

    @Column(name = "validation_rules", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    String validationRules;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (orderIndex == null || orderIndex < 0) {
            throw new IllegalArgumentException("Element order index must not be negative");
        }
        if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
            throw new IllegalArgumentException("Min value must not exceed max value");
        }
        if (minCount != null && maxCount != null && minCount > maxCount) {
            throw new IllegalArgumentException("Min count must not exceed max count");
        }
        if (maxSizeMb != null && maxSizeMb < 1) {
            throw new IllegalArgumentException("Max size must be at least 1 MB");
        }
    }
}
