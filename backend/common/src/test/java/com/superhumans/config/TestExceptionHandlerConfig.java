package com.superhumans.config;

import com.superhumans.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GlobalExceptionHandler.class)
public class TestExceptionHandlerConfig {
}