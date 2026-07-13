package com.superhumans.dto;

import com.superhumans.entity.UserRole;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private UUID id;
    private String login;
    private String fullName;
    private UserRole role;
    private String email;
    private String specialityCode;
    private String specialityName;
    private String phone;
}
