package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Parses and builds the immutable template snapshot (JSONB) attached to a flow
 * instance at creation time. Execution logic never reads the mutable template
 * tree directly - it works against the snapshot, so later template edits can
 * never affect already-started instances.
 */
@Component
public class TemplateSnapshotParser {

    private final ObjectMapper objectMapper;

    public TemplateSnapshotParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SnapshotTemplate parse(String json) {
        try {
            return objectMapper.readValue(json, SnapshotTemplate.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Template snapshot is corrupted");
        }
    }

    public String toJson(SnapshotTemplate snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalArgumentException("Template snapshot could not be serialized");
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SnapshotTemplate {
        String name;
        Integer version;
        String productType;
        String amputationLevel;
        String limbSide;
        Integer estimatedDurationMin;
        List<SnapshotStage> stages;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SnapshotStage {
        UUID id;
        String name;
        String stageType;
        boolean canSkip;
        boolean requiresApproval;
        SnapshotGate gate;
        List<SnapshotStep> steps;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SnapshotGate {
        UUID id;
        String name;
        String requiredApproverRole;
        List<String> checklist;
        boolean attachmentsRequired;
        List<SnapshotReworkLoop> reworkLoops;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SnapshotReworkLoop {
        UUID targetStepId;
        String reworkType;
        int maxAttempts;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SnapshotStep {
        UUID id;
        String name;
        String stepType;
        boolean mandatory;
        boolean allowBackward;
        boolean autoStartTimer;
        Integer normDurationMin;
        List<SnapshotElement> elements;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SnapshotElement {
        UUID id;
        String elementType;
        String label;
        boolean required;
        String unit;
        BigDecimal minValue;
        BigDecimal maxValue;
        Integer minCount;
        Integer maxCount;
        String regexPattern;
        List<String> options;
        List<String> mimeTypes;
        Integer maxSizeMb;
    }
}
