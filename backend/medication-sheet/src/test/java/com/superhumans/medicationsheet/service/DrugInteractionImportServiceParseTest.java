package com.superhumans.medicationsheet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superhumans.exception.BadRequestException;
import com.superhumans.medicationsheet.service.DrugInteractionImportService.ImportError;
import com.superhumans.medicationsheet.service.DrugInteractionImportService.Parsed;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class DrugInteractionImportServiceParseTest {

    private final DrugInteractionImportService service =
            new DrugInteractionImportService(null, null, null, null);

    @Test
    void parse_deduplicatesMirroredReferences() {
        String json = """
                {"drugs":[
                  {"id":"1","atc_code":"A01AA01","generic_en":"A","ukrainian_raw":"лік А","confidence":0.9,
                   "drug_interactions":[{"atc_code":"B01AB01","severity":"high","interaction":"текст","interaction_id":"i1"}]},
                  {"id":"2","atc_code":"B01AB01","generic_en":"B","ukrainian_raw":"лік Б","confidence":0.8,
                   "drug_interactions":[{"atc_code":"A01AA01","severity":"high","interaction":"текст","interaction_id":"i1-mirror"}]}
                ]}
                """;
        Parsed parsed = service.parse(json.getBytes(StandardCharsets.UTF_8));
        assertThat(parsed.drugs()).hasSize(2);
        assertThat(parsed.pairs()).hasSize(1);
        assertThat(parsed.pairs().get(0).getDrugAAtc()).isEqualTo("A01AA01");
        assertThat(parsed.pairs().get(0).getDrugBAtc()).isEqualTo("B01AB01");
        assertThat(parsed.pairs().get(0).getInteractionId()).isEqualTo("i1");
        assertThat(parsed.skipped()).isEmpty();
    }

    @Test
    void parse_keepsDistinctSeverityForSamePair() {
        String json = """
                {"drugs":[
                  {"id":"1","atc_code":"A01AA01","ukrainian_raw":"А",
                   "drug_interactions":[
                     {"atc_code":"B01AB01","severity":"medium","interaction":"текст 1","interaction_id":"a"},
                     {"atc_code":"B01AB01","severity":"high","interaction":"текст 2","interaction_id":"b"}]},
                  {"id":"2","atc_code":"B01AB01","ukrainian_raw":"Б"}
                ]}
                """;
        Parsed parsed = service.parse(json.getBytes(StandardCharsets.UTF_8));
        assertThat(parsed.pairs()).hasSize(2);
        assertThat(parsed.pairs()).extracting(p -> p.getSeverity()).containsExactly("high", "medium");
    }

    @Test
    void parse_atcCollisionPrefersHighestConfidence() {
        String json = """
                {"drugs":[
                  {"id":"1","atc_code":"A01AA01","ukrainian_raw":"низька впевненість","confidence":0.4},
                  {"id":"2","atc_code":"A01AA01","ukrainian_raw":"висока впевненість","confidence":0.9}
                ]}
                """;
        Parsed parsed = service.parse(json.getBytes(StandardCharsets.UTF_8));
        assertThat(parsed.drugs()).hasSize(1);
        assertThat(parsed.drugs().get(0).getUkrainianRaw()).isEqualTo("висока впевненість");
    }

    @Test
    void parse_skipsMalformedDrugsAndCountsThem() {
        String json = """
                {"drugs":[
                  {"id":"1","atc_code":"wrong","ukrainian_raw":"поганий ATC"},
                  {"id":"2","atc_code":"A01AA01"},
                  {"id":"3","atc_code":"B01AB01","ukrainian_raw":"ок","confidence":"не-число"}
                ]}
                """;
        Parsed parsed = service.parse(json.getBytes(StandardCharsets.UTF_8));
        assertThat(parsed.drugs()).hasSize(1);
        assertThat(parsed.skipped()).hasSize(2);
        assertThat(parsed.skipped())
                .anySatisfy(s -> assertThat(s).contains("некоректний atc_code"))
                .anySatisfy(s -> assertThat(s).contains("порожній ukrainian_raw"));
    }

    @Test
    void parse_skipsMalformedInteractionRefs() {
        String json = """
                {"drugs":[
                  {"id":"1","atc_code":"A01AA01","ukrainian_raw":"А",
                   "drug_interactions":[
                     {"atc_code":"B01AB01","severity":"bogus","interaction":"текст"},
                     {"atc_code":"B01AB01","severity":"high"},
                     {"atc_code":"A01AA01","severity":"high","interaction":"себе"},
                     {"atc_code":"Z99ZZ9","severity":"high","interaction":"невідомий ATC"}]},
                  {"id":"2","atc_code":"B01AB01","ukrainian_raw":"Б"}
                ]}
                """;
        Parsed parsed = service.parse(json.getBytes(StandardCharsets.UTF_8));
        assertThat(parsed.pairs()).isEmpty();
        assertThat(parsed.skipped()).hasSize(3);
    }

    @Test
    void parse_rejectsEmptyDrugListAndBadJson() {
        assertThatThrownBy(() -> service.parse("{\"drugs\":[]}".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.parse("не json".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void parse_rejectsWhenNoValidDrugs() {
        String json = """
                {"drugs":[{"id":"1","atc_code":"wrong","ukrainian_raw":"x"}]}
                """;
        ImportError error = org.assertj.core.api.Assertions
                .catchThrowableOfType(() -> service.parse(json.getBytes(StandardCharsets.UTF_8)), ImportError.class);
        assertThat(error).isNotNull();
        assertThat(error.getSkippedDetails()).hasSize(1);
    }

    @Test
    void rowHash_isHex64AndStable() {
        String hash = DrugInteractionImportService.sha256Hex("A01AA1|B01AB1|high|текст".getBytes(StandardCharsets.UTF_8));
        assertThat(hash).matches("[0-9a-f]{64}");
        assertThat(hash).isEqualTo(DrugInteractionImportService.sha256Hex("A01AA1|B01AB1|high|текст".getBytes(StandardCharsets.UTF_8)));
        assertThat(hash).isNotEqualTo(DrugInteractionImportService.sha256Hex("A01AA1|B01AB1|high|інший".getBytes(StandardCharsets.UTF_8)));
    }
}
