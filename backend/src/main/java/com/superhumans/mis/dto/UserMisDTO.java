package com.superhumans.mis.dto;

import lombok.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserMisDTO {
    UUID id;
    String login;
    String fullName;
    String shortName;
    String specialityCode;
    String specialityName;
    String email;
    String phone;
}
