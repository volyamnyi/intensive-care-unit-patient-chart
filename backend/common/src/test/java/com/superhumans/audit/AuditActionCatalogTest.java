package com.superhumans.audit;

import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActionDefinition.Flag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditActionCatalogTest {

    private static final Pattern ACTION_CODE =
            Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*){2,5}$");

    @Test
    void catalog_containsAllAtomicModuleActions() {
        assertThat(AuditActionCatalog.definitions()).hasSize(119);
        assertThat(AuditActionCatalog.forModule("platform")).hasSize(19);
        assertThat(AuditActionCatalog.forModule("icu")).hasSize(42);
        assertThat(AuditActionCatalog.forModule("medication")).hasSize(26);
        assertThat(AuditActionCatalog.forModule("prosthetics")).hasSize(32);
    }

    @Test
    void everyActionHasUniqueVersionedFormatModuleFlagsAndPolicy() {
        var definitions = AuditActionCatalog.definitions();
        Set<String> codes = new HashSet<>();

        for (var definition : definitions) {
            assertThat(codes.add(definition.code()))
                    .as("action code %s must be unique", definition.code())
                    .isTrue();
            assertThat(definition.code()).matches(ACTION_CODE);
            assertThat(definition.code()).startsWith(definition.module() + ".");
            assertThat(definition.area()).isNotBlank();
            assertThat(definition.flags()).isNotEmpty();
            assertThat(definition.actorPolicy()).isNotNull();
            assertThat(definition.dataClass()).isNotNull();
        }

        assertThat(AuditActionCatalog.byCode()).hasSize(definitions.size());
    }

    @Test
    void registrySeparatesSecurityAndUserActivityFromBusinessActions() {
        assertThat(AuditActionCatalog.require("platform.auth.session.login").eventClass())
                .isEqualTo(EventClass.SECURITY);
        assertThat(AuditActionCatalog.require("platform.patient.record.view").eventClass())
                .isEqualTo(EventClass.USER_ACTIVITY);
        assertThat(AuditActionCatalog.require("icu.episode.create").eventClass())
                .isEqualTo(EventClass.BUSINESS);
        assertThat(AuditActionCatalog.definitions())
                .allMatch(definition -> definition.eventClass() != EventClass.TECHNICAL);
    }

    @Test
    void registryDoesNotTreatActionIdAsTechnicalHttpMethod() {
        assertThat(AuditActionCatalog.definitions())
                .noneMatch(definition -> definition.code().matches(".*\\.api_(get|post|put|patch|delete)$"));
    }

    @Test
    void mandatoryActionsHaveExplicitClassificationFlags() {
        assertThat(AuditActionCatalog.definitions())
                .filteredOn(definition -> definition.flags().contains(Flag.MANDATORY))
                .allSatisfy(definition -> {
                    assertThat(definition.module()).isNotBlank();
                    assertThat(definition.actorPolicy()).isNotNull();
                    assertThat(definition.dataClass()).isNotNull();
                });
    }

    @Test
    void unknownActionFailsClosed() {
        assertThatThrownBy(() -> AuditActionCatalog.require("icu.api_post"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown audit action");
    }
}
