package com.superhumans.entity.core;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import com.superhumans.entity.base.BaseEntity;

@Entity
@Table(name = "system_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemSettings extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    String value;

    @Column(columnDefinition = "TEXT")
    String description;
}
