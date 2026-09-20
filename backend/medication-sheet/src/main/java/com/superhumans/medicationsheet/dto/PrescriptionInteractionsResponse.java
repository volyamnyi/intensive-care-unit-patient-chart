package com.superhumans.medicationsheet.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionInteractionsResponse {
    List<ItemWarning> warnings;
    MissingAtcNotice missingAtc;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ItemWarning {
        UUID itemId;
        String nameUk;
        List<PairWarning> interactions;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PairWarning {
        UUID otherItemId;
        String otherNameUk;
        String severity;
        String interactionText;
        LocalDate overlapStart;
        LocalDate overlapEnd;
        List<String> interactionIds;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MissingAtcNotice {
        boolean present;
        List<String> names;
    }
}
