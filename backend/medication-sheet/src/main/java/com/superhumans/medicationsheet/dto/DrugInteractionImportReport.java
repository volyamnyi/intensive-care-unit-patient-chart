package com.superhumans.medicationsheet.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DrugInteractionImportReport {
    int drugs;
    int interactions;
    int skipped;
    List<String> skippedDetails;
    long durationMs;
    String sourceHash;
}
