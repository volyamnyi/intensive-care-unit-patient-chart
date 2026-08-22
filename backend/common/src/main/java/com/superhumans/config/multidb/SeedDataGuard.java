package com.superhumans.config.multidb;

import static java.util.Arrays.asList;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail-fast boot guard: refuses to start under the {@code prod} profile when
 * seed-data bootstrapping is enabled. First-boot seeding creates well-known
 * demo credentials, so seeding in production would keep them permanently valid
 * (audit finding A2, CWE-798). The production profile disables seeding via
 * {@code app.seed-data.enabled: false}; this guard is the defense-in-depth
 * tripwire that catches a misconfiguration such as {@code APP_SEED_DATA_ENABLED}
 * forced to {@code true} before any seed SQL executes.
 */
@Component
public class SeedDataGuard implements InitializingBean {

    private final boolean seedDataEnabled;
    private final List<String> activeProfiles;

    public SeedDataGuard(@Value("${app.seed-data.enabled:true}") boolean seedDataEnabled,
            Environment environment) {
        this.seedDataEnabled = seedDataEnabled;
        this.activeProfiles = asList(environment.getActiveProfiles());
    }

    @Override
    public void afterPropertiesSet() {
        if (seedDataEnabled && activeProfiles.contains("prod")) {
            throw new IllegalStateException(
                    "PROD protection: app.seed-data.enabled is true while the 'prod' profile is "
                    + "active. Seed-data bootstrapping is forbidden in production: it issues "
                    + "well-known demo credentials and would permanently reset user passwords. "
                    + "Set app.seed-data.enabled=false (or APP_SEED_DATA_ENABLED=false).");
        }
    }
}
