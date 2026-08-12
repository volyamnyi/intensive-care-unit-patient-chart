package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignRequest {
    @NotNull
    Long userId;
    String hash;
    String certSerialNumber;
    String certIssuer;
    String certSubject;
    LocalDateTime certValidFrom;
    LocalDateTime certValidUntil;

    public SignRequest(Long userId, String hash) {
        this.userId = userId;
        this.hash = hash;
    }
}
