package com.superhumans.medicationsheet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.entity.core.SystemSettings;
import com.superhumans.medicationsheet.dto.DrugInteractionImportReport;
import com.superhumans.medicationsheet.entity.DrugInteractionDrug;
import com.superhumans.medicationsheet.entity.DrugInteractionPair;
import com.superhumans.medicationsheet.repository.DrugInteractionDrugRepository;
import com.superhumans.medicationsheet.repository.DrugInteractionPairRepository;
import com.superhumans.repository.core.SystemSettingsRepository;
import com.superhumans.service.AuditService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full-sync import of the drug-interactions dataset (issue #304).
 *
 * <p>Source of truth is a JSON document with a {@code drugs[]} root; each drug
 * carries {@code id}, {@code atc_code}, {@code generic_en},
 * {@code ukrainian_raw}, {@code confidence} and an {@code drug_interactions[]}
 * list. The sync is a single transaction: parse & validate everything first,
 * then wipe both tables and insert — the database becomes exactly the JSON.
 * Any failure rolls the whole thing back.
 *
 * <p>Dedup rules: drug rows are keyed by ATC (highest {@code confidence}
 * wins, first occurrence breaks exact ties); interaction rows are canonical
 * unordered ATC pairs keyed by (drugA, drugB, severity, rowHash), where
 * {@code rowHash} = sha256(a|b|severity|interaction text) so mirrored
 * A→B/B→A references collapse into one row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DrugInteractionImportService {

    static final Pattern ATC_PATTERN = Pattern.compile("^[A-Z][0-9]{2}[A-Z]{2}[0-9]{2}$");
    static final Set<String> SEVERITIES = Set.of("low", "medium", "high", "critical");

    public static final String LAST_IMPORT_KEY = "drug_interactions.last_import_at";

    ObjectMapper objectMapper = new ObjectMapper();
    DrugInteractionDrugRepository drugRepository;
    DrugInteractionPairRepository pairRepository;
    SystemSettingsRepository settingsRepository;
    AuditService auditService;

    /** Thrown when the document parses to zero rows; carries the skip detail list. */
    @Getter
    public static class ImportError extends RuntimeException {
        final List<String> skippedDetails;

        public ImportError(List<String> skippedDetails) {
            super("Drug-interactions import rejected");
            this.skippedDetails = skippedDetails;
        }
    }

    public record Parsed(List<DrugInteractionDrug> drugs, List<DrugInteractionPair> pairs,
                         List<String> skipped) { }

    @Transactional("medTransactionManager")
    public DrugInteractionImportReport importDataset(byte[] content, Long adminId) {
        long startedAt = System.currentTimeMillis();
        String sourceHash = sha256Hex(content);
        Parsed parsed = parse(content);

        pairRepository.deleteAllPairs();
        drugRepository.deleteAllDrugs();
        drugRepository.saveAll(parsed.drugs());
        pairRepository.saveAll(parsed.pairs());

        long durationMs = System.currentTimeMillis() - startedAt;
        DrugInteractionImportReport report = DrugInteractionImportReport.builder()
                .drugs(parsed.drugs().size())
                .interactions(parsed.pairs().size())
                .skipped(parsed.skipped().size())
                .skippedDetails(parsed.skipped())
                .durationMs(durationMs)
                .sourceHash(sourceHash)
                .build();

        SystemSettings lastImport = settingsRepository.findByKey(LAST_IMPORT_KEY)
                .orElseGet(() -> SystemSettings.builder().key(LAST_IMPORT_KEY).build());
        lastImport.setValue(LocalDateTime.now().toString());
        if (lastImport.getDescription() == null) {
            lastImport.setDescription("Timestamp of the last drug-interactions dataset import");
        }
        settingsRepository.save(lastImport);
        auditService.logEvent("DrugInteractions", null, "IMPORT", adminId,
                null, jsonOf(report), "drug-interactions-import");
        log.info("Drug-interactions import: drugs={}, pairs={}, skipped={}, {} ms",
                parsed.drugs().size(), parsed.pairs().size(), parsed.skipped().size(), durationMs);
        return report;
    }

    /** Parse + dedup without touching the database; throws {@link ImportError} when nothing is valid. */
    public Parsed parse(byte[] content) {
        JsonNode root;
        try {
            root = objectMapper.readTree(content);
        } catch (Exception e) {
            throw new BadRequestException("Некоректний JSON файлу взаємодій");
        }
        JsonNode drugsNode = root.get("drugs");
        if (drugsNode == null || !drugsNode.isArray() || drugsNode.isEmpty()) {
            throw new BadRequestException("JSON має містити непорожній масив «drugs»");
        }

        List<String> skipped = new ArrayList<>();
        Map<String, DrugInteractionDrug> byAtc = new HashMap<>();
        Set<String> knownAtcs = new HashSet<>();

        for (JsonNode drugNode : drugsNode) {
            String atc = text(drugNode, "atc_code");
            if (atc != null && ATC_PATTERN.matcher(atc).matches()) {
                knownAtcs.add(atc);
            }
        }

        List<DrugInteractionPair> pairAccumulator = new ArrayList<>();

        int index = 0;
        for (JsonNode drugNode : drugsNode) {
            index++;
            String atc = text(drugNode, "atc_code");
            String ukName = text(drugNode, "ukrainian_raw");
            if (atc == null || !ATC_PATTERN.matcher(atc).matches()) {
                skipped.add("drugs[" + (index - 1) + "]: некоректний atc_code");
                continue;
            }
            if (ukName == null) {
                skipped.add("drugs[" + (index - 1) + "]: порожній ukrainian_raw");
                continue;
            }

            DrugInteractionDrug existing = byAtc.get(atc);
            DrugInteractionDrug candidate = DrugInteractionDrug.builder()
                    .atcCode(atc)
                    .sourceId(text(drugNode, "id"))
                    .genericEn(text(drugNode, "generic_en"))
                    .ukrainianRaw(ukName)
                    .confidence(decimal(drugNode, "confidence"))
                    .build();
            if (existing == null || confidenceOf(candidate) > confidenceOf(existing)) {
                byAtc.put(atc, candidate);
            }

            JsonNode interactions = drugNode.get("drug_interactions");
            int ref = 0;
            if (interactions != null && interactions.isArray()) {
                for (JsonNode refNode : interactions) {
                    ref++;
                    String location = "drugs[" + (index - 1) + "].drug_interactions[" + (ref - 1) + "]";
                    String otherAtc = text(refNode, "atc_code");
                    String severity = text(refNode, "severity");
                    String interactionText = text(refNode, "interaction");
                    String interactionId = text(refNode, "interaction_id");
                    if (otherAtc == null || !ATC_PATTERN.matcher(otherAtc).matches()) {
                        skipped.add(location + ": некоректний atc_code");
                        continue;
                    }
                    if (severity == null || !SEVERITIES.contains(severity)) {
                        skipped.add(location + ": некоректний severity");
                        continue;
                    }
                    if (interactionText == null) {
                        skipped.add(location + ": порожній interaction");
                        continue;
                    }
                    if (atc.equals(otherAtc)) {
                        continue;
                    }
                    if (!knownAtcs.contains(otherAtc)) {
                        log.warn("Interaction reference to unknown ATC '{}' (ignored)", otherAtc);
                        continue;
                    }
                    String a = atc.compareTo(otherAtc) < 0 ? atc : otherAtc;
                    String b = atc.compareTo(otherAtc) < 0 ? otherAtc : atc;
                    pairAccumulator.add(DrugInteractionPair.builder()
                            .drugAAtc(a)
                            .drugBAtc(b)
                            .severity(severity)
                            .interaction(interactionText)
                            .interactionId(interactionId)
                            .interactionConfidence(decimal(refNode, "confidence"))
                            .rowHash(sha256Hex((a + "|" + b + "|" + severity + "|" + interactionText)
                                    .getBytes(StandardCharsets.UTF_8)))
                            .build());
                }
            }
        }

        List<DrugInteractionDrug> drugs = new ArrayList<>(byAtc.values());
        drugs.sort(Comparator.comparing(DrugInteractionDrug::getAtcCode));

        Map<String, DrugInteractionPair> dedup = new HashMap<>();
        for (DrugInteractionPair pair : pairAccumulator) {
            String key = pair.getDrugAAtc() + "" + pair.getDrugBAtc() + ""
                    + pair.getSeverity() + "" + pair.getRowHash();
            dedup.putIfAbsent(key, pair);
        }
        List<DrugInteractionPair> pairs = dedup.values().stream()
                .sorted(Comparator.comparing(DrugInteractionPair::getDrugAAtc)
                        .thenComparing(DrugInteractionPair::getDrugBAtc)
                        .thenComparing(DrugInteractionPair::getSeverity)
                        .thenComparing(DrugInteractionPair::getRowHash))
                .toList();

        if (drugs.isEmpty()) {
            throw new ImportError(skipped);
        }
        return new Parsed(drugs, pairs, skipped);
    }

    private static double confidenceOf(DrugInteractionDrug drug) {
        return drug.getConfidence() == null ? 0.0 : drug.getConfidence().doubleValue();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String s = value.asText().trim();
        return s.isEmpty() ? null : s;
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String jsonOf(DrugInteractionImportReport report) {
        try {
            Map<String, Object> payload = new TreeMap<>();
            payload.put("drugs", report.getDrugs());
            payload.put("interactions", report.getInteractions());
            payload.put("skipped", report.getSkipped());
            payload.put("sourceHash", report.getSourceHash());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"drugs\":" + report.getDrugs();
        }
    }
}
