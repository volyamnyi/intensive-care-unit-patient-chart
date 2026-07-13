package com.superhumans.dto;

import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    private String token;
    private UUID userId;
    private String login;
    private String fullName;
    private String role;
    private String email;
}
