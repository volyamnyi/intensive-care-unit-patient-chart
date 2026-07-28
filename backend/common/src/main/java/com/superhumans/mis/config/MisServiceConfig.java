package com.superhumans.mis.config;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MisServiceConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
