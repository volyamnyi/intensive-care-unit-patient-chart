package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prescription_signatures")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionSignature extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    PrescriptionItem item;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(nullable = false, length = 32)
    String role;

    @Column(name = "signed_at", nullable = false)
    LocalDateTime signedAt;

    @Column(length = 255)
    String hash;

    @Column(length = 32)
    String status;
}
