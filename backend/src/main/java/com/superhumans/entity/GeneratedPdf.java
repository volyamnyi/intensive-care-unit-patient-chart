package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "generated_pdfs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GeneratedPdf extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    private ClinicalDay clinicalDay;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_version", nullable = false)
    private Integer fileVersion;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "generated_by", nullable = false)
    private UUID generatedBy;

    @Column(length = 256)
    private String checksum;
}
