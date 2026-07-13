package com.superhumans.mis.dto;

import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepartmentDTO {
    private UUID id;
    private String name;
    private String code;
}
