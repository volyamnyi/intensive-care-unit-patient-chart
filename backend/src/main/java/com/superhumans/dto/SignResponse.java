package com.superhumans.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SignResponse {
    private UUID signatureId;
    private UUID clinicalDayId;
    private String role;
    private LocalDateTime signedAt;
    private String hash;
    private Integer version;
}
