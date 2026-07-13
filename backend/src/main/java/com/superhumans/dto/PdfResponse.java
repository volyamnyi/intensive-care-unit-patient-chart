package com.superhumans.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PdfResponse {
    private UUID id;
    private UUID clinicalDayId;
    private String fileName;
    private Integer fileVersion;
    private LocalDateTime generatedAt;
    private UUID generatedBy;
    private String checksum;
}
