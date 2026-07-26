package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ErrorResponse {
    String code;
    String message;
    String correlationId;
}
