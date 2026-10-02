package com.superhumans.entity.core;

import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/** Composite key for primary, parent and related entity references in audit history. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditEventTargetId implements Serializable {

    private static final long serialVersionUID = 1L;

    UUID auditId;
    String relationType;
    String entityType;
    String entityId;
}
