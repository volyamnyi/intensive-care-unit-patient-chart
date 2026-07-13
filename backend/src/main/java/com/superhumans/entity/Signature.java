package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signatures")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Signature extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    private ClinicalDay clinicalDay;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt;

    @Column(length = 256)
    private String hash;

    @Column(nullable = false, length = 20)
    private String status;
}
