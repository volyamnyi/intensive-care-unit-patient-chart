package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    String login;
    String password;
}
