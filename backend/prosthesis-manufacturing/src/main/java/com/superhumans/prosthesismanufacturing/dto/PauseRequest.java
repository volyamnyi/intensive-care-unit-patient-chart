package com.superhumans.prosthesismanufacturing.dto;

import com.superhumans.prosthesismanufacturing.entity.PauseCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PauseRequest {
    @NotNull
    PauseCategory category;
}
