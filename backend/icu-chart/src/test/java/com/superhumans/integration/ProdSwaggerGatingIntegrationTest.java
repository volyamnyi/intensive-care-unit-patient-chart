package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Phase B pin: under the {@code prod} profile springdoc is disabled, so the
 * Swagger permit rules disappear with it (issue #171 / #188). The docs
 * surface must NOT be publicly readable: unauthenticated requests hit
 * {@code authenticated()} and are rejected with 401 rather than served.
 *
 * <p>Standalone overrides keep the hardened prod defaults bootable in CI:
 * TLS off (no keystore), seeding off (SeedDataGuard), and a non-default JWT
 * secret (JwtSecretGuard fails fast on the committed default under prod).
 */
@TestPropertySource(properties = {
        "server.ssl.enabled=false",
        "app.seed-data.enabled=false",
        "app.jwt.secret=cHJvZC10ZXN0LXNlY3JldC12YWx1ZS1mb3Itc3dhZ2dlci1nYXRlLTAwMDAwMDA",
        "app.mis.embedded-wiremock-enabled=false"
})
@ActiveProfiles("prod")
class ProdSwaggerGatingIntegrationTest extends AbstractIntegrationTest {

    @Test
    void apiDocs_unauthenticated_underProd_isNotPubliclyReadable() {
        var res = restTemplate.exchange("/api-docs", HttpMethod.GET,
                authGet(null), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void swaggerUi_unauthenticated_underProd_isNotPubliclyReadable() {
        var res = restTemplate.exchange("/swagger-ui/index.html", HttpMethod.GET,
                authGet(null), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
