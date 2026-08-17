package com.superhumans.prosthesismanufacturing.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FailureSnapshotResponse {
    UUID id;
    UUID instanceId;
    String category;
    String description;
    String snapshot;
    Long createdBy;
    LocalDateTime createdAt;
}
