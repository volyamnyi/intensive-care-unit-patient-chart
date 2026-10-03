package com.superhumans.audit;

import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.ChangeSet;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Parses the new Liquibase masters without connecting to or mutating any database. */
class AuditChangelogContractTest {

    private static final List<ExpectedChangeset> EXPECTED = List.of(
            new ExpectedChangeset("db.changelog-master-core.yaml", "009-audit-events.sql", "9", "split-core"),
            new ExpectedChangeset("db.changelog-master-core.yaml", "010-audit-security-access.sql", "10", "split-core"),
            new ExpectedChangeset("db.changelog-master-core.yaml", "011-audit-legacy-backfill.sql", "11", "split-core"),
            new ExpectedChangeset("db.changelog-master-icu.yaml", "079-audit-outbox.sql", "79", "split-icu"),
            new ExpectedChangeset("db.changelog-master-med.yaml", "027-audit-outbox.sql", "27", "split-med"),
            new ExpectedChangeset("db.changelog-master-prosth.yaml", "033-audit-outbox.sql", "33", "split-prosth"));

    @Test
    void eachModuleMasterIncludesItsAuditChangesetExactlyOnce() throws Exception {
        var accessor = new ClassLoaderResourceAccessor();
        var parserFactory = ChangeLogParserFactory.getInstance();

        for (ExpectedChangeset expected : EXPECTED) {
            String path = "db/changelog/" + expected.masterFile();
            DatabaseChangeLog changeLog = parserFactory.getParser(path, accessor)
                    .parse(path, new ChangeLogParameters(), accessor);
            List<ChangeSet> matches = changeLog.getChangeSets().stream()
                    .filter(changeSet -> changeSet.getFilePath().endsWith(expected.sqlFile())
                            && changeSet.getId().equals(expected.changesetId()))
                    .toList();

            assertThat(matches)
                    .as("changeset %s in %s", expected.changesetId(), path)
                    .hasSize(1);
            assertThat(matches.get(0).getId()).isEqualTo(expected.changesetId());
            assertThat(matches.get(0).getAuthor()).isEqualTo(expected.author());
        }
    }

    private record ExpectedChangeset(String masterFile, String sqlFile,
                                     String changesetId, String author) {
    }
}
