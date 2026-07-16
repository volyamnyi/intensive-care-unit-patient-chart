package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "generated_pdfs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeneratedPdf extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    ClinicalDay clinicalDay;

    @Column(name = "file_name", nullable = false, length = 500)
    String fileName;

    @Column(name = "file_version", nullable = false)
    Integer fileVersion;

    @Column(name = "generated_at", nullable = false)
    LocalDateTime generatedAt;

    @Column(name = "generated_by", nullable = false)
    UUID generatedBy;

    @Column(length = 256)
    String checksum;
}
