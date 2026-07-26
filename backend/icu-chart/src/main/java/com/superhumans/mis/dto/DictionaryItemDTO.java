package com.superhumans.mis.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DictionaryItemDTO {
    String code;
    String name;
}
