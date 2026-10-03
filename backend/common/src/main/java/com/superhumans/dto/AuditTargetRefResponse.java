package com.superhumans.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Indexed object reference of an audit event (primary, parent or related). */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditTargetRefResponse {
    String relationType;
    String entityType;
    String entityId;
    String businessKey;
}
