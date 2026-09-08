package com.superhumans.mis.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * MIS HTTP client wiring.
 * <p>
 * Provides the shared {@link RestTemplate} for MIS API calls with configurable
 * connect and read timeouts.
 */
@Configuration
public class MisServiceConfig {

    @Bean
    public RestTemplate restTemplate(
            @Value("${app.mis.api.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.mis.api.read-timeout-ms:15000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return new RestTemplate(factory);
    }
}
