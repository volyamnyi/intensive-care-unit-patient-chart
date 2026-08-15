package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import com.superhumans.entity.base.BaseEntity;

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

    @Column(name = "cert_serial_number", length = 64)
    String certSerialNumber;

    @Column(name = "cert_issuer", length = 256)
    String certIssuer;

    @Column(name = "cert_subject", length = 256)
    String certSubject;

    @Column(name = "cert_valid_from")
    LocalDateTime certValidFrom;

    @Column(name = "cert_valid_until")
    LocalDateTime certValidUntil;

    @Column(nullable = false, length = 20)
    String status;
}
