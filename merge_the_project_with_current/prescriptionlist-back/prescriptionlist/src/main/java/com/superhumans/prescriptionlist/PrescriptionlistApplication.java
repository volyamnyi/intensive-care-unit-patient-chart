package com.superhumans.prescriptionlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"com.superhumans.controller","com.superhumans.service","com.superhumans.repository","com.superhumans.config"})
public class PrescriptionlistApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrescriptionlistApplication.class, args);
	}

}
