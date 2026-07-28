package com.superhumans.medicationsheet.config;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("com.superhumans.medicationsheet")
@EnableJpaRepositories({"com.superhumans.medicationsheet.repository", "com.superhumans.repository"})
@EntityScan({"com.superhumans.medicationsheet.entity", "com.superhumans.entity"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicationSheetConfig {
}
