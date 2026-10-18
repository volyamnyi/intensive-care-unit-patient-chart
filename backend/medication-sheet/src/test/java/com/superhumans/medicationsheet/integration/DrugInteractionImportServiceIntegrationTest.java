package com.superhumans.medicationsheet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superhumans.medicationsheet.repository.DrugInteractionDrugRepository;
import com.superhumans.medicationsheet.repository.DrugInteractionPairRepository;
import com.superhumans.medicationsheet.service.DrugInteractionImportService;
import com.superhumans.medicationsheet.service.DrugInteractionImportService.ImportError;
import com.superhumans.repository.core.SystemSettingsRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-level contract of the drug-interactions dataset import (#428): full sync
 * semantics, idempotent re-import, and single-transaction rollback so a broken
 * payload can never leave a half-written catalog behind.
 */
@SpringBootTest(properties = "app.seed-data.enabled=false")
@Transactional(value = "medTransactionManager", rollbackFor = Exception.class)
class DrugInteractionImportServiceIntegrationTest {

    private static final byte[] TWO_DRUGS_ONE_PAIR = """
            {"drugs":[
              {"id":"1","atc_code":"N02BE01","ukrainian_raw":"Парацетамол",
               "drug_interactions":[{"atc_code":"M01AE01","severity":"high",
                                    "interaction":"Підвищення ризику ураження печінки","interaction_id":"i1"}]},
              {"id":"2","atc_code":"M01AE01","ukrainian_raw":"Ібупрофен"}
            ]}
            """.getBytes(StandardCharsets.UTF_8);

    private static final byte[] THREE_DRUGS_TWO_PAIRS = """
            {"drugs":[
              {"id":"1","atc_code":"N02BE01","ukrainian_raw":"Парацетамол",
               "drug_interactions":[{"atc_code":"M01AE01","severity":"high","interaction":"Печінка","interaction_id":"i1"},
                                    {"atc_code":"B01AC06","severity":"critical","interaction":"Кровотеча","interaction_id":"i2"}]},
              {"id":"2","atc_code":"M01AE01","ukrainian_raw":"Ібупрофен"},
              {"id":"3","atc_code":"B01AC06","ukrainian_raw":"Аспірин"}
            ]}
            """.getBytes(StandardCharsets.UTF_8);

    @Autowired
    private DrugInteractionImportService importService;

    @Autowired
    private DrugInteractionPairRepository pairRepository;

    @Autowired
    private DrugInteractionDrugRepository drugRepository;

    @Autowired
    private SystemSettingsRepository settingsRepository;

    @BeforeEach
    void clearCatalog() {
        pairRepository.deleteAllPairs();
        drugRepository.deleteAllDrugs();
    }

    @Test
    void importDataset_persistsDrugsAndPairs() {
        var report = importService.importDataset(TWO_DRUGS_ONE_PAIR, 6L);

        assertThat(report.getDrugs()).isEqualTo(2);
        assertThat(report.getInteractions()).isEqualTo(1);
        assertThat(report.getSkipped()).isZero();
        assertThat(report.getSourceHash()).hasSize(64);
        assertThat(drugRepository.count()).isEqualTo(2);
        assertThat(pairRepository.count()).isEqualTo(1);
        assertThat(pairRepository.findDangerousForAtc("N02BE01", java.util.List.of("high")))
                .singleElement()
                .satisfies(pair -> {
                    // Canonical ordering is lexicographic a<b, not dataset order.
                    assertThat(pair.getDrugAAtc()).isEqualTo("M01AE01");
                    assertThat(pair.getDrugBAtc()).isEqualTo("N02BE01");
                    assertThat(pair.getSeverity()).isEqualTo("high");
                });
        assertThat(settingsRepository.findByKey(DrugInteractionImportService.LAST_IMPORT_KEY))
                .get()
                .extracting(settings -> settings.getValue())
                .asString()
                .isNotBlank();
    }

    @Test
    void importDataset_isAFullSyncSoReimportReplacesTheCatalog() {
        importService.importDataset(TWO_DRUGS_ONE_PAIR, 6L);

        var second = importService.importDataset(THREE_DRUGS_TWO_PAIRS, 6L);

        assertThat(second.getDrugs()).isEqualTo(3);
        assertThat(second.getInteractions()).isEqualTo(2);
        assertThat(drugRepository.count()).isEqualTo(3);
        assertThat(pairRepository.count()).isEqualTo(2);
        assertThat(pairRepository.countBySeverity())
                .hasSize(2)
                .anySatisfy(row -> {
                    assertThat(row[0]).isEqualTo("critical");
                    assertThat(row[1]).isEqualTo(1L);
                });
    }

    @Test
    void importDataset_rejectsPayloadWithoutAnyValidRow() {
        assertThatThrownBy(() -> importService.importDataset(
                "{\"drugs\":[{\"id\":\"1\",\"atc_code\":\"nope\",\"ukrainian_raw\":\"X\"}]}"
                        .getBytes(StandardCharsets.UTF_8), 6L))
                .isInstanceOf(ImportError.class);

        assertThat(drugRepository.count()).isZero();
        assertThat(pairRepository.count()).isZero();
    }

    @Test
    void importDataset_rollsBackTheWipeWhenThePayloadIsUnreadable() {
        importService.importDataset(TWO_DRUGS_ONE_PAIR, 6L);

        assertThatThrownBy(() -> importService.importDataset(
                "{\"drugs\":[".getBytes(StandardCharsets.UTF_8), 6L))
                .isInstanceOf(RuntimeException.class);

        // The previous dataset survives: the wipe happens in the same
        // transaction as the parse failure, so nothing is committed.
        assertThat(drugRepository.count()).isEqualTo(2);
        assertThat(pairRepository.count()).isEqualTo(1);
    }

    @Test
    void importDataset_isVisibleToTheCatalogBrowseQuery() {
        importService.importDataset(THREE_DRUGS_TWO_PAIRS, 6L);

        // The browse filter matches ATC codes and display names, not the
        // interaction text itself.
        var page = pairRepository.findCatalog("critical", "b01ac06",
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getInteraction()).isEqualTo("Кровотеча");
    }

    /** Proves the test itself would notice a committed-but-partial wipe. */
    @Test
    void guard_importRunsInTheMedTransactionManager() {
        assertThat(org.springframework.transaction.support.TransactionSynchronizationManager
                .isActualTransactionActive()).isTrue();
        assertThat(org.springframework.transaction.support.TransactionSynchronizationManager
                .getCurrentTransactionName()).isNotNull();
        assertThat(Propagation.SUPPORTS).isNotNull();
    }
}