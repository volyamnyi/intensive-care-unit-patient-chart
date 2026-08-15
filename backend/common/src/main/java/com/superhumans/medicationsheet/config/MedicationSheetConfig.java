package com.superhumans.medicationsheet.config;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.superhumans.medicationsheet")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicationSheetConfig {
}
