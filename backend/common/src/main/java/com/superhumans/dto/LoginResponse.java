package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    String token;
    Long userId;
    String login;
    String fullName;
    String role;
    String email;
    String permissions;
}
