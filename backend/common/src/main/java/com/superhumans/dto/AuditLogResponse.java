package com.superhumans.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogResponse {
    UUID id;
    LocalDateTime timestamp;
    Long userId;
    String entity;
    UUID entityId;
    String action;
    String oldValue;
    String newValue;
    String correlationId;
    String ipAddress;
    String userRole;
}
