package com.superhumans.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignResponse {
    UUID signatureId;
    UUID clinicalDayId;
    String role;
    LocalDateTime signedAt;
    String hash;
    String certSerialNumber;
    String certIssuer;
    String certSubject;
    LocalDateTime certValidFrom;
    LocalDateTime certValidUntil;
    Integer version;
}
