package com.superhumans.mis.dto;

import lombok.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepartmentDTO {
    UUID id;
    String name;
    String code;
}
