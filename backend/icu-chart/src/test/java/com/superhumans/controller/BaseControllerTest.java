package com.superhumans.controller;

import com.superhumans.config.TestCommonConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = TestCommonConfig.class)
public abstract class BaseControllerTest {
}
