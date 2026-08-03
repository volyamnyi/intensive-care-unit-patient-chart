package com.superhumans.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(com.superhumans.config.TestExceptionHandlerConfig.class)
public class TestCommonConfig {
}