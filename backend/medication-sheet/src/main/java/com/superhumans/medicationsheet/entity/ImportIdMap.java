package com.superhumans.medicationsheet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Old-ID to new-ID mapping (staging, table {@code import_id_map}).
 * The {@code PRIMARY KEY (old_kind, old_id)} is the idempotency guard:
 * a re-run skips everything already mapped. Kinds: LIST, MED_EL, MED_DAY,
 * VIT_LIST, VIT_DAY, VIT_ENTRY. Day-part cells have no rows on purpose
 * (deleted/rolled back through their mapped parents).
 */
@Entity
@Table(name = "import_id_map")
@IdClass(ImportIdMapId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportIdMap {

    @Id
    @Column(name = "old_kind", nullable = false, length = 16, updatable = false)
    String oldKind;

    @Id
    @Column(name = "old_id", nullable = false, length = 64, updatable = false)
    String oldId;

    @Column(name = "new_id", nullable = false, updatable = false)
    UUID newId;

    @Column(name = "run_id", nullable = false, updatable = false)
    UUID runId;
}
