package com.superhumans.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableAsync
public class WebConfig implements WebMvcConfigurer {

    // Ensures JSON responses declare a UTF-8 charset so that non-browser
    // clients (e.g. API tests, external integrators) decode Cyrillic text
    // correctly instead of falling back to the platform default codepage.
    // We only adjust the existing Jackson converter so the ObjectMapper
    // configured by Spring Boot (date formatting, etc.) is preserved.
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jackson) {
                jackson.setDefaultCharset(StandardCharsets.UTF_8);
                jackson.setSupportedMediaTypes(List.of(
                        new MediaType("application", "json", StandardCharsets.UTF_8),
                        new MediaType("application", "*+json", StandardCharsets.UTF_8)));
            }
        }
    }
}
