package com.superhumans;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatientChartApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientChartApplication.class, args);
    }
}