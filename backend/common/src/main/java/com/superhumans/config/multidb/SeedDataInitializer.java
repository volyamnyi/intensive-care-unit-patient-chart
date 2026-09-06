package com.superhumans.config.multidb;

import com.superhumans.service.UserSeedService;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed-data.enabled", havingValue = "true", matchIfMissing = true)
@DependsOn({"seedDataGuard", "coreLiquibase", "icuLiquibase", "medLiquibase", "prosthLiquibase"})
public class SeedDataInitializer implements InitializingBean {

    private final DataSource coreDataSource;
    private final DataSource icuDataSource;
    private final DataSource medDataSource;
    private final DataSource prosthDataSource;
    private final UserSeedService userSeedService;

    public SeedDataInitializer(@Qualifier("coreDataSource") DataSource coreDataSource,
            @Qualifier("icuDataSource") DataSource icuDataSource,
            @Qualifier("medDataSource") DataSource medDataSource,
            @Qualifier("prosthDataSource") DataSource prosthDataSource,
            UserSeedService userSeedService) {
        this.coreDataSource = coreDataSource;
        this.icuDataSource = icuDataSource;
        this.medDataSource = medDataSource;
        this.prosthDataSource = prosthDataSource;
        this.userSeedService = userSeedService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        execute(coreDataSource, "data-core.sql");
        execute(icuDataSource, "data-icu.sql");
        execute(medDataSource, "data-med.sql");
        execute(prosthDataSource, "data-prosth.sql");
        // Application users come from APP_TEST_* environment variables
        // (UserSeedService), never from SQL scripts.
        userSeedService.seedFromEnvironment();
    }

    private static void execute(DataSource dataSource, String script) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(new ClassPathResource(script), StandardCharsets.UTF_8));
        }
    }
}