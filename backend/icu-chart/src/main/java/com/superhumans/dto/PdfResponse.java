package com.superhumans.dto;

import com.superhumans.entity.TransferStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PdfResponse {
    UUID id;
    UUID clinicalDayId;
    String fileName;
    Integer fileVersion;
    LocalDateTime generatedAt;
    Long generatedBy;
    String checksum;
    TransferStatus transferStatus;
    LocalDateTime transferredAt;
    String transferError;
}
