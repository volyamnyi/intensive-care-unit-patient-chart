package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superhumans.medicationsheet.entity.VitalSignList;
import com.superhumans.medicationsheet.repository.VitalSignListRepository;
import com.superhumans.medicationsheet.service.VitalSignService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * #382 — the read-then-INSERT races, proven with real concurrency.
 *
 * <p>Two threads provision the same fresh list through independent transactions,
 * released together by a latch. Before the unique index + convergence, the loser
 * either errored with a generic 409 or created a duplicate 106-row grid. The
 * contract now: both callers get the SAME grid and exactly one grid exists.
 */
class RaceGuardConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private VitalSignService vitalSignService;
    @Autowired
    private VitalSignListRepository vitalListRepository;
    @Autowired
    @Qualifier("medTransactionManager")
    private PlatformTransactionManager medTx;

    private UUID createFreshList() {
        var res = restTemplate.exchange("/api/prescriptions", HttpMethod.POST,
                authEntity("{\"patientId\":\"1003\"}", getDoctorToken()),
                com.superhumans.medicationsheet.dto.PrescriptionListResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        return res.getBody().getId();
    }

    @Test
    void concurrentGetOrCreate_convergesOnOneGrid() throws Exception {
        UUID listId = createFreshList();
        try {
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch start = new CountDownLatch(1);
            try {
                // No test-level transaction wrapper: the service owns its own
                // med-leg transaction per call (exactly how the controller
                // reaches it), which is what lets the loser converge.
                List<Future<VitalSignList>> futures = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    futures.add(pool.submit(() -> {
                        start.await();
                        return vitalSignService.getOrCreate(listId);
                    }));
                }
                start.countDown();
                VitalSignList first = futures.get(0).get(10, TimeUnit.SECONDS);
                VitalSignList second = futures.get(1).get(10, TimeUnit.SECONDS);

                // Both callers see the same grid - no duplicate, no 409 to a user
                // who did nothing wrong, no second 106-row provisioning.
                assertThat(first).isNotNull();
                assertThat(second).isNotNull();
                assertThat(second.getId()).isEqualTo(first.getId());
                assertThat(vitalListRepository.findByPrescriptionListId(listId)).isPresent();
                assertThat(vitalListRepository.findAll().stream()
                        .filter(l -> listId.equals(l.getPrescriptionList().getId()))
                        .count()).isEqualTo(1);
            } finally {
                pool.shutdownNow();
            }
        } finally {
            restTemplate.exchange("/api/prescriptions/{id}", HttpMethod.DELETE,
                    authEntity(null, getDoctorToken()), Void.class, listId);
        }
    }

    @Test
    void theUniqueIndexBacksTheConvergence() {
        // The concurrency behaviour above is only as good as the index behind
        // it: a manual duplicate insert must be rejected by the database.
        UUID listId = createFreshList();
        try {
            vitalSignService.getOrCreate(listId);
            var first = vitalListRepository.findByPrescriptionListId(listId).orElseThrow();

            VitalSignList duplicate = VitalSignList.builder()
                    .prescriptionList(first.getPrescriptionList())
                    .build();
            duplicate.setCreatedBy(0L);
            duplicate.setUpdatedBy(0L);

            assertThatThrownBy(() -> new TransactionTemplate(medTx)
                    .executeWithoutResult(status -> vitalListRepository.saveAndFlush(duplicate)))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        } finally {
            restTemplate.exchange("/api/prescriptions/{id}", HttpMethod.DELETE,
                    authEntity(null, getDoctorToken()), Void.class, listId);
        }
    }

    @Test
    void concurrentEpisodeCreation_leavesOneActiveEpisode_andTheLoserGetsTheDomainContract() throws Exception {
        // #382: the partial unique index uq_episodes_active_patient backs the
        // one-ACTIVE-episode-per-patient rule. Two concurrent creates: the
        // winner gets 201, the loser gets the SAME 422 EPISODE_ALREADY_ACTIVE
        // contract as the sequential duplicate - not a generic 409.
        long patientId = 977_777L;
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    var body = new com.superhumans.dto.EpisodeCreateRequest(
                            patientId, UUID.randomUUID(), null,
                            java.time.LocalDateTime.now(), null, null, null, null, null);
                    var res = restTemplate.exchange("/api/episodes", HttpMethod.POST,
                            new org.springframework.http.HttpEntity<>(body, authHeaders(getDoctorToken())),
                            String.class);
                    return res.getStatusCode().value();
                }));
            }
            start.countDown();
            int first = results.get(0).get(15, TimeUnit.SECONDS);
            int second = results.get(1).get(15, TimeUnit.SECONDS);

            assertThat(List.of(first, second))
                    .describedAs("one create wins with 201, the loser gets 422 - in either order")
                    .containsExactlyInAnyOrder(201, 422);

            // And exactly one ACTIVE episode exists for the patient.
            var search = restTemplate.exchange("/api/episodes?patientId={patientId}", HttpMethod.GET,
                    authGet(getDoctorToken()), String.class, patientId);
            assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(search.getBody()).isNotNull();
            assertThat(search.getBody().split("\"status\"").length - 1).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}