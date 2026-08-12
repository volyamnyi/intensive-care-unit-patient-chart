package com.superhumans.dto;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloseEarlyRequest {
    String reason;
}
