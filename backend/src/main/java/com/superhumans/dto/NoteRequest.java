package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class NoteRequest {
    private String content;
    private String noteType;
}
