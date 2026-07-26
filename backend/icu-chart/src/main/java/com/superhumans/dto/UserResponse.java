package com.superhumans.dto;

import com.superhumans.entity.UserRole;
import lombok.*;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    Long id;
    String login;
    String fullName;
    UserRole role;
    String email;
    String specialityCode;
    String specialityName;
    String phone;
}
