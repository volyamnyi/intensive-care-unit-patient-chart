package com.superhumans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PatientChartApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientChartApplication.class, args);
    }
}
