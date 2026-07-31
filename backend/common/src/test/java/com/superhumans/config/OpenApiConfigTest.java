package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for the property-driven {@link OpenApiConfig} introduced by the
 * config-move refactoring. All {@code app.api.*} properties are injected via
 * {@code @Value} and are therefore exercised through reflection.
 */
class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(config, "title", "ICU Test API");
        ReflectionTestUtils.setField(config, "version", "2.0.0");
        ReflectionTestUtils.setField(config, "description", "Test description");
        ReflectionTestUtils.setField(config, "contactName", "Test Team");
        ReflectionTestUtils.setField(config, "contactEmail", "team@test.ua");
        ReflectionTestUtils.setField(config, "licenseName", "Apache-2.0");
        ReflectionTestUtils.setField(config, "licenseUrl", "https://www.apache.org/licenses/LICENSE-2.0");
    }

    @Test
    void customOpenAPI_reflectsConfiguredProperties() {
        OpenAPI api = config.customOpenAPI();

        Info info = api.getInfo();
        assertThat(info).isNotNull();
        assertThat(info.getTitle()).isEqualTo("ICU Test API");
        assertThat(info.getVersion()).isEqualTo("2.0.0");
        assertThat(info.getDescription()).isEqualTo("Test description");
        assertThat(info.getContact()).isNotNull();
        assertThat(info.getContact().getName()).isEqualTo("Test Team");
        assertThat(info.getContact().getEmail()).isEqualTo("team@test.ua");
        assertThat(info.getLicense()).isNotNull();
        assertThat(info.getLicense().getName()).isEqualTo("Apache-2.0");
        assertThat(info.getLicense().getUrl()).isEqualTo("https://www.apache.org/licenses/LICENSE-2.0");
    }

    @Test
    void customOpenAPI_includesBearerSecurityScheme() {
        OpenAPI api = config.customOpenAPI();

        assertThat(api.getSecurity()).hasSize(1);
        assertThat(api.getSecurity().get(0)).containsKey("Bearer Authentication");

        SecurityScheme scheme = api.getComponents().getSecuritySchemes().get("Bearer Authentication");
        assertThat(scheme).isNotNull();
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    void customOpenAPI_withNullProperties_doesNotThrow() {
        ReflectionTestUtils.setField(config, "title", null);
        ReflectionTestUtils.setField(config, "version", null);
        ReflectionTestUtils.setField(config, "description", null);
        ReflectionTestUtils.setField(config, "contactName", null);
        ReflectionTestUtils.setField(config, "contactEmail", null);
        ReflectionTestUtils.setField(config, "licenseName", null);
        ReflectionTestUtils.setField(config, "licenseUrl", null);

        assertThatCode(config::customOpenAPI).doesNotThrowAnyException();
    }
}
