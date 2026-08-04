package com.superhumans.prosthesismanufacturing.config;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("com.superhumans.prosthesismanufacturing")
@EnableJpaRepositories("com.superhumans.prosthesismanufacturing.repository")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProsthesisManufacturingConfig {
}
