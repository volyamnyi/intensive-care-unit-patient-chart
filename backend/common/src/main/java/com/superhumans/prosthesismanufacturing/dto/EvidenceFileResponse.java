package com.superhumans.prosthesismanufacturing.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EvidenceFileResponse {
    UUID id;
    UUID stepExecutionId;
    String fileName;
    String mimeType;
    Long sizeBytes;
    String checksum;
    LocalDateTime createdAt;
}
