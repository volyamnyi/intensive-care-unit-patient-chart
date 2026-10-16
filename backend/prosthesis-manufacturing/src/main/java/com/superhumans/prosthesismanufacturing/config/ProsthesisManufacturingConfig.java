package com.superhumans.prosthesismanufacturing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
// Intentional no-op marker (#427 item 8): the package sits under the root
// scan, so this changes nothing — it documents module ownership.
@ComponentScan("com.superhumans.prosthesismanufacturing")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProsthesisManufacturingConfig {

    /**
     * Intentionally raw and load-bearing (#427 item 8): Spring Boot backs
     * off its own Jackson mapper while any {@code ObjectMapper} bean exists,
     * so every injected mapper app-wide is THIS one. Template snapshots are
     * written and re-read with it (numeric dates both ways); swapping in a
     * JavaTimeModule mapper would change stored snapshot bytes. Migrate only
     * with a snapshot-compatibility proof, never by deleting this bean.
     */
    @Bean
    public ObjectMapper prosthesisObjectMapper() {
        return new ObjectMapper();
    }
}
