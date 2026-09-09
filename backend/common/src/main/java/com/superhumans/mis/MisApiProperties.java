package com.superhumans.mis;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Real MIS API configuration ({@code app.mis.api.*}).
 * <p>
 * Secrets arrive exclusively through environment variables ({@code APP_MIS_API_*});
 * no defaults exist for them — a blank value means "not configured" and is reported
 * (by key name only) when a call is actually attempted. Non-secret structural knobs
 * (paths, timeouts, skew) carry safe defaults.
 * <p>
 * Agents and logs must reference variable NAMES only, never values.
 */
@Getter
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MisApiProperties {

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
     * Fail-fast validation: reports missing property KEYS only — never values, so
     * the message is safe for startup logs. Intended to be invoked right before the
     * real MIS is touched (token fetch or method call) so local/test contexts that
     * never call the real MIS still boot cleanly.
     *
     * @throws IllegalStateException when required keys are blank or malformed
     */
    public void ensureConfigured() {
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
                    "MIS API misconfigured, missing: " + String.join(", ", missing));
        }
    }
}
