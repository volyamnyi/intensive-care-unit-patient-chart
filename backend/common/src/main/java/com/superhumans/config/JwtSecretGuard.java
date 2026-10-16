package com.superhumans.config;

import static java.util.Arrays.asList;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail-fast boot guard for the JWT signing secret (audit finding A1,
 * CWE-798/321). The secret is env-required: {@code application.yml} declares
 * a bare {@code ${APP_JWT_SECRET}} placeholder with no default, so a missing
 * variable fails boot at placeholder resolution before this guard even runs.
 * This guard is belt-and-braces: under the {@code prod} profile it refuses to
 * start when the resolved secret still equals the retired committed default
 * (anyone with repo read access could otherwise forge tokens for any role).
 * Generate with e.g. {@code openssl rand -base64 64}.
 */
@Component
public class JwtSecretGuard implements InitializingBean {

    // SHA-256 of the retired committed secret (#372 item 5): the plaintext
    // must not survive in the codebase — if a yml default is ever
    // reintroduced, the app would sign with a publicly-known key.
    static final String RETIRED_SECRET_SHA256 =
            "c6d96e93a8dd8c22d2367ea20eda36f677a12710cbb523a7a2f79e985a30fcaf";

    private final String secret;
    private final List<String> activeProfiles;

    public JwtSecretGuard(@Value("${app.jwt.secret}") String secret, Environment environment) {
        this.secret = secret;
        this.activeProfiles = asList(environment.getActiveProfiles());
    }

    @Override
    public void afterPropertiesSet() {
        if (activeProfiles.contains("prod") && RETIRED_SECRET_SHA256.equals(sha256Hex(secret))) {
            throw new IllegalStateException("PROD protection: app.jwt.secret is the retired committed default. "
                    + "Set APP_JWT_SECRET in the prod environment (e.g. openssl rand -base64 64) so "
                    + "tokens cannot be forged with the publicly-committed secret.");
        }
    }

    static String sha256Hex(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
