package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
    private String correlationId;
}
