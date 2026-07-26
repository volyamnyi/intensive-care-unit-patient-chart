package com.superhumans.dto;

import com.superhumans.entity.UserRole;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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
