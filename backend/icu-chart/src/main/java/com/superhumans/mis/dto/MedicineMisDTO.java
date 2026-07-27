package com.superhumans.mis.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicineMisDTO {
    Long id;
    String name;
    Integer categoryRef;
    String ptgCode;
}
