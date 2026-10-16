package com.superhumans.medicationsheet.config;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

// Intentional no-op marker (#427 item 8): the package sits under the root
// scan, so this changes nothing — it documents module ownership.
@Configuration
@ComponentScan("com.superhumans.medicationsheet")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicationSheetConfig {
}
