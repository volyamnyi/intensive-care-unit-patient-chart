package com.superhumans.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.superhumans.config.multidb.CoreDatabaseConfig;
import com.superhumans.config.multidb.IcuDatabaseConfig;
import com.superhumans.config.multidb.MedicationDatabaseConfig;
import com.superhumans.config.multidb.ProstheticsDatabaseConfig;

/**
 * String-coupling guard (#427 item 1c): the multi-DB configs embed feature
 * package names as annotation strings, invisible to bytecode analysis (and to
 * ArchUnit). A package rename must fail a test, not the boot. Lives in the
 * app module — the only test tree seeing every feature package.
 */
class MultidbPackageStringsTest {

    @Test
    void repositoryBasePackages_resolveOnClasspath() throws Exception {
        List<String> packages = new ArrayList<>();
        for (Class<?> config : List.of(
                CoreDatabaseConfig.class,
                IcuDatabaseConfig.class,
                MedicationDatabaseConfig.class,
                ProstheticsDatabaseConfig.class)) {
            EnableJpaRepositories annotation = config.getAnnotation(EnableJpaRepositories.class);
            assertThat(annotation)
                    .describedAs("expected @EnableJpaRepositories on " + config.getSimpleName())
                    .isNotNull();
            packages.addAll(List.of(annotation.basePackages()));
        }

        assertThat(packages).hasSize(4);
        ClassLoader loader = getClass().getClassLoader();
        for (String pkg : packages) {
            boolean present = Collections.list(
                    loader.getResources(pkg.replace('.', '/'))).size() > 0;
            assertThat(present)
                    .describedAs("repository base package resolves on classpath: " + pkg)
                    .isTrue();
        }
    }
}
