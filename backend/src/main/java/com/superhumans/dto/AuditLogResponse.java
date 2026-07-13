package com.superhumans.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLogResponse {
    private UUID id;
    private LocalDateTime timestamp;
    private UUID userId;
    private String entity;
    private UUID entityId;
    private String action;
    private String oldValue;
    private String newValue;
    private String correlationId;
}
