package com.superhumans.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginResponse {
    /**
     * Access token is also set in the httpOnly cookie by AuthController.
     * Retained in the JSON response for supported Bearer-token API clients;
     * browser UI authentication uses the cookie and does not store this value.
     */
    String token;
    Long userId;
    String login;
    String fullName;
    String role;
    String email;
}
