package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.health.contributor.Status;

/** #412 item 7: the app must not report UP while MIS is completely down. */
class MisHealthIndicatorTest {

    private MisHealthIndicator indicator() {
        MisApiProperties properties = Mockito.mock(MisApiProperties.class);
        when(properties.getBaseUrl()).thenReturn("https://mis.invalid");
        return new MisHealthIndicator(properties);
    }

    @Test
    void upBeforeAnyCall() {
        var health = indicator().health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("misConfigured", true)
                .containsEntry("lastError", "none");
    }

    @Test
    void downWhileTheLastFailureIsRecent() {
        MisHealthIndicator indicator = indicator();

        indicator.recordFailure("HTTP 503");

        var health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("lastError", "HTTP 503")
                .containsEntry("failures", 1L);
    }

    @Test
    void aSuccessClearsTheFailure() {
        MisHealthIndicator indicator = indicator();
        indicator.recordFailure("timeout");

        indicator.recordSuccess();

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getDetails()).containsEntry("lastError", "none");
    }

    @Test
    void aStaleFailureDoesNotKeepTheServiceDownForever() {
        MisApiProperties properties = Mockito.mock(MisApiProperties.class);
        when(properties.getBaseUrl()).thenReturn("https://mis.invalid");
        // Fixed clock: the failure is recorded, then the window is stepped past.
        java.time.Instant start = java.time.Instant.parse("2026-10-16T08:00:00Z");
        MisHealthIndicator indicator = new MisHealthIndicator(properties,
                java.time.Clock.fixed(start, java.time.ZoneOffset.UTC));
        indicator.recordFailure("transport");

        MisHealthIndicator later = new MisHealthIndicator(properties,
                java.time.Clock.fixed(start.plus(MisHealthIndicator.FAILURE_WINDOW).plusSeconds(30),
                        java.time.ZoneOffset.UTC));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(later.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void detailsNeverLeakCredentials() {
        var details = indicator().health().getDetails();

        assertThat(details.keySet())
                .doesNotContain("password", "token", "login", "installationGuid");
    }
}
