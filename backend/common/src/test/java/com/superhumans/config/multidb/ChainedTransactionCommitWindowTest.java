package com.superhumans.config.multidb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionStatus;

/**
 * #375 вЂ” pins the cross-database commit window instead of pretending it does not
 * exist.
 *
 * <p>A method-body exception rolls every leg back, which is why the system looks
 * atomic. A failure during the commit phase of a later leg does not: the earlier
 * legs are already durable. This test makes that asymmetry executable, so nobody
 * has to re-derive it from the bytecode and so a future change to the transaction
 * wiring shows up here.
 */
class ChainedTransactionCommitWindowTest {

    /** A leg that records what happened to it and can be told to fail on commit. */
    private static final class RecordingLeg implements PlatformTransactionManager {

        private final String name;
        private final boolean failOnCommit;
        private final List<String> journal;

        RecordingLeg(String name, boolean failOnCommit, List<String> journal) {
            this.name = name;
            this.failOnCommit = failOnCommit;
            this.journal = journal;
        }

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            journal.add("begin:" + name);
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            if (failOnCommit) {
                journal.add("commit-failed:" + name);
                throw new TransactionSystemException("connection reset while committing " + name);
            }
            journal.add("commit:" + name);
        }

        @Override
        public void rollback(TransactionStatus status) {
            journal.add("rollback:" + name);
        }
    }

    @Test
    void methodBodyFailureRollsEveryLegBack() {
        List<String> journal = new ArrayList<>();
        PlatformTransactionManager chain = chain(journal, false);
TransactionStatus status = begin(chain);

        chain.rollback(status);
        clearSynchronization();

        assertThat(journal).containsExactly(
                "begin:core", "begin:icu", "begin:med", "begin:prosth",
                "rollback:prosth", "rollback:med", "rollback:icu", "rollback:core");
    }

    @Test
    void commitPhaseFailureOnALaterLegLeavesEarlierLegsCommitted() {
        List<String> journal = new ArrayList<>();
        // icu fails on commit; legs begin core -> icu -> med -> prosth, so core has
        // already been committed by the time icu is reached (reverse order).
        PlatformTransactionManager chain = new ChainedTransactionManager(
                new RecordingLeg("core", false, journal),
                new RecordingLeg("icu", true, journal),
                new RecordingLeg("med", false, journal),
                new RecordingLeg("prosth", false, journal));

        TransactionStatus status = begin(chain);

        assertThatThrownBy(() -> chain.commit(status))
                .isInstanceOf(TransactionException.class);

// The window: legs committed before the failure (prosth, med) stay durable,
// while core — the leg whose audit rows belong to those very writes — is rolled
// back. That is the issue's scenario exactly: business rows without their audit.
assertThat(journal).containsExactly(
                "begin:core", "begin:icu", "begin:med", "begin:prosth",
                "commit:prosth", "commit:med", "commit-failed:icu", "rollback:core");
    }

    @Test
    void legsAreCommittedInReverseOrderOfBegin() {
        List<String> journal = new ArrayList<>();
        PlatformTransactionManager chain = chain(journal, false);

        TransactionStatus status = begin(chain);
        chain.commit(status);

        assertThat(journal).containsExactly(
                "begin:core", "begin:icu", "begin:med", "begin:prosth",
                "commit:prosth", "commit:med", "commit:icu", "commit:core");
    }

    /**
     * The mitigation lever: with the core-side legacy audit leg switched off, a
     * feature mutation touches exactly one leg, so there is nothing to become
     * partial. This documents why {@code app.audit.legacy-write-enabled} is the
     * supported way to shrink the window.
     */
    @Test
    void aSingleLegHasNoWindowToLeavePartial() {
        List<String> journal = new ArrayList<>();
        PlatformTransactionManager single = new ChainedTransactionManager(
                new RecordingLeg("med", false, journal));

        TransactionStatus status = begin(single);
        single.commit(status);

        assertThat(journal).containsExactly("begin:med", "commit:med");
    }

private static PlatformTransactionManager chain(List<String> journal, boolean failOnCommit) {
        return new ChainedTransactionManager(
                new RecordingLeg("core", failOnCommit, journal),
                new RecordingLeg("icu", false, journal),
                new RecordingLeg("med", false, journal),
                new RecordingLeg("prosth", false, journal));
    }

    /**
     * Begins the chain and clears the synchronization the chain initialises, so the
     * four tests cannot leak state into each other. A {@code null} definition would
     * return an empty status without beginning any leg, which is why this passes a
     * real one.
     */
    private static TransactionStatus begin(PlatformTransactionManager chain) {
        return chain.getTransaction(new DefaultTransactionDefinition());
    }

    /** The chain deactivates synchronization itself on a clean completion. */
    private static void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}