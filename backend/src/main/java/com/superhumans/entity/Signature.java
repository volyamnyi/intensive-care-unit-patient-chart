package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "signatures")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Signature extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    ClinicalDay clinicalDay;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(nullable = false, length = 20)
    String role;

    @Column(name = "signed_at", nullable = false)
    LocalDateTime signedAt;

    @Column(length = 256)
    String hash;

    @Column(nullable = false, length = 20)
    String status;
}
