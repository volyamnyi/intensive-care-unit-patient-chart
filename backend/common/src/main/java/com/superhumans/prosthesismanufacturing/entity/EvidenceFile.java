package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "prosthetics_evidence_files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EvidenceFile extends BaseEntity {

    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_execution_id", nullable = false)
    StepExecution stepExecution;

    @Column(name = "file_name", nullable = false)
    String fileName;

    @Column(name = "mime_type", nullable = false, length = 100)
    String mimeType;

    @Column(name = "size_bytes", nullable = false)
    Long sizeBytes;

    @Column(name = "checksum", length = 64)
    String checksum;

    @Column(name = "file_data", nullable = false)
    byte[] fileData;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (sizeBytes == null || sizeBytes < 0) {
            throw new IllegalArgumentException("File size must not be negative");
        }
        if (sizeBytes > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size must not exceed 10 MB");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("MIME type is required");
        }
        if (!mimeType.startsWith("image/") && !"application/pdf".equals(mimeType)) {
            throw new IllegalArgumentException("Only image and PDF files are allowed");
        }
    }
}
