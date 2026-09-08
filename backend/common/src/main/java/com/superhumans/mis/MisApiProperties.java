package com.superhumans.mis;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MIS API configuration ({@code app.mis.*}).
 * <p>
 * Secrets arrive exclusively through environment variables ({@code APP_MIS_API_*});
 * no defaults exist for them. Non-secret knobs (paths, timeouts) carry
 * safe defaults. Agents and logs must reference variable NAMES only, never values.
 */
@Getter
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MisApiProperties {

    @Value("${app.mis.api.base-url:http://localhost:9090}")
    String baseUrl;

    @Value("${app.mis.api.run-path:/api/run}")
    String runPath;

    @Value("${app.mis.login:integration}")
    String login;

    @Value("${app.mis.api.password:}")
    String password;

    @Value("${app.mis.api.installation-guid:}")
    String installationGuid;

    @Value("${app.mis.api.connect-timeout-ms:5000}")
    int connectTimeoutMs;

    @Value("${app.mis.api.read-timeout-ms:15000}")
    int readTimeoutMs;
}
