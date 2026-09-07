package com.superhumans.mis;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Real MIS API configuration ({@code app.mis.*}).
 * <p>
 * Secrets arrive exclusively through environment variables ({@code APP_MIS_API_*});
 * no defaults exist for them. Non-secret knobs (paths, timeouts, skew) carry
 * safe defaults. Agents and logs must reference variable NAMES only, never values.
 */
@Getter
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MisApiProperties {

    @Value("${app.mis.mode:wiremock}")
    String mode;

    @Value("${app.mis.api.base-url:}")
    String baseUrl;

    @Value("${app.mis.api.token-path:/token}")
    String tokenPath;

    @Value("${app.mis.api.run-path:/api/run}")
    String runPath;

    @Value("${app.mis.api.login:}")
    String login;

    @Value("${app.mis.api.password:}")
    String password;

    @Value("${app.mis.api.installation-guid:}")
    String installationGuid;

    @Value("${app.mis.api.connect-timeout-ms:5000}")
    int connectTimeoutMs;

    @Value("${app.mis.api.read-timeout-ms:15000}")
    int readTimeoutMs;

    @Value("${app.mis.api.token-skew-sec:60}")
    int tokenSkewSec;

    /**
     * Whether the client must talk to the real MIS API instead of WireMock.
     *
     * @return true only for {@code app.mis.mode=real} (case-insensitive)
     */
    public boolean isRealMode() {
        return "real".equalsIgnoreCase(mode);
    }

    /**
     * Fail-fast validation for real mode. Reports missing property KEYS only —
     * never values, so the message is safe for startup logs.
     *
     * @throws IllegalStateException when real mode is on and required keys are missing
     */
    public void assertRealModeReady() {
        if (!isRealMode()) {
            return;
        }
        List<String> missing = new ArrayList<>();
        if (baseUrl == null || baseUrl.isBlank()) {
            missing.add("app.mis.api.base-url");
        }
        if (tokenPath == null || tokenPath.isBlank() || !tokenPath.startsWith("/")) {
            missing.add("app.mis.api.token-path");
        }
        if (runPath == null || runPath.isBlank() || !runPath.startsWith("/")) {
            missing.add("app.mis.api.run-path");
        }
        if (login == null || login.isBlank()) {
            missing.add("app.mis.api.login");
        }
        if (password == null || password.isBlank()) {
            missing.add("app.mis.api.password");
        }
        if (installationGuid == null || installationGuid.isBlank()) {
            missing.add("app.mis.api.installation-guid");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Real MIS API misconfigured, missing: " + String.join(", ", missing));
        }
    }
}
