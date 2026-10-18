package com.superhumans.config;

import com.superhumans.exception.GlobalExceptionHandler;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Test-support annotation (#371 item 5): moved out of {@code src/main} into the
 * shared common test-jar, so the production JAR no longer ships it.
 * Feature-module MVC slices consume it through the {@code classifier=tests}
 * dependency.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(GlobalExceptionHandler.class)
public @interface EnableTestExceptionHandler {
}