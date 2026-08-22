package com.superhumans.config;

import static java.util.Arrays.asList;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fail-fast boot guard: refuses to start under the {@code prod} profile when
 * the JWT signing secret is still the committed default value. Anyone with repo
 * read access could otherwise forge tokens for any role (audit finding A1,
 * CWE-798/321). The production deployment must override it via
 * {@code APP_JWT_SECRET} (e.g. {@code openssl rand -base64 64}).
 */
@Component
public class JwtSecretGuard implements InitializingBean {

    static final String DEFAULT_SECRET =
            "cGF0aWVudC1jaGFydC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi0yMDI2";

    private final String secret;
    private final List<String> activeProfiles;

    public JwtSecretGuard(@Value("${app.jwt.secret}") String secret, Environment environment) {
        this.secret = secret;
        this.activeProfiles = asList(environment.getActiveProfiles());
    }

    @Override
    public void afterPropertiesSet() {
        if (activeProfiles.contains("prod") && DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("PROD protection: app.jwt.secret is the committed default. "
                    + "Set APP_JWT_SECRET in the prod environment (e.g. openssl rand -base64 64) so "
                    + "tokens cannot be forged with the publicly-committed secret.");
        }
    }
}
