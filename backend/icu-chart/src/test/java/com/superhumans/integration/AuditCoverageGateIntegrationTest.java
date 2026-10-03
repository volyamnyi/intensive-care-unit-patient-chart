package com.superhumans.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.superhumans.audit.AuditActionCatalog;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

class AuditCoverageGateIntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    private JsonNode readRegistry(String name) {
        try (var stream = getClass().getResourceAsStream("/" + name)) {
            assertThat(stream).as("registry %s present", name).isNotNull();
            return mapper.readTree(stream);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read registry " + name, exception);
        }
    }

    @Test
    void catalog_isSelfConsistent_v1_120Codes() {
        var definitions = AuditActionCatalog.definitions();
        assertThat(definitions).hasSize(120);
        Set<String> codes = new HashSet<>();
        for (var definition : definitions) {
            assertThat(codes.add(definition.code()))
                    .as("duplicate catalog code %s", definition.code()).isTrue();
            assertThat(definition.code()).matches("[a-z0-9]+(\\.[a-z0-9_]+){2,5}");
            assertThat(definition.module()).isNotBlank();
            assertThat(definition.area()).isNotBlank();
            assertThat(definition.actionType()).isNotNull();
            assertThat(definition.eventClass()).isNotNull();
            assertThat(definition.actorPolicy()).isNotNull();
            assertThat(definition.dataClass()).isNotNull();
            assertThat(definition.criticality()).isNotNull();
        }
        assertThat(AuditActionCatalog.byCode()).hasSize(120);
    }

    @Test
    void everyCatalogAction_hasExactlyOneRegistryEntry_withResolvableTests() {
        JsonNode registry = readRegistry("audit-action-coverage.json");
        assertThat(registry.get("version").asInt()).isEqualTo(1);
        Map<String, JsonNode> byAction = new HashMap<>();
        for (JsonNode entry : registry.get("actions")) {
            String action = entry.get("action").asText();
            assertThat(byAction.put(action, entry)).as("duplicate registry action %s", action).isNull();
        }
        for (var definition : AuditActionCatalog.definitions()) {
            JsonNode entry = byAction.get(definition.code());
            assertThat(entry).as("catalog action without coverage entry: %s", definition.code()).isNotNull();
            boolean excluded = entry.has("excluded");
            boolean mapped = entry.has("tests");
            assertThat(excluded ^ mapped)
                    .as("action %s must have either tests or excluded, not both/neither", definition.code())
                    .isTrue();
            if (mapped) {
                assertThat(entry.get("level").asText()).isIn("emission", "operation");
                assertThat(entry.get("tests").size()).isGreaterThanOrEqualTo(1);
                for (JsonNode test : entry.get("tests")) {
                    assertThatTestSourceExists(test);
                }
            } else {
                assertThat(entry.get("excluded").asText()).isNotBlank();
            }
        }
    }

    @Test
    void everyMutatingApiRoute_isPinnedInTheRouteRegistry() {
        JsonNode registry = readRegistry("audit-route-coverage.json");
        assertThat(registry.get("version").asInt()).isEqualTo(1);
        Map<String, JsonNode> byRoute = new HashMap<>();
        for (JsonNode entry : registry.get("routes")) {
            String key = entry.get("method").asText() + " " + entry.get("path").asText();
            assertThat(byRoute.put(key, entry)).as("duplicate registry route %s", key).isNull();
            if (entry.has("actions")) {
                for (JsonNode action : entry.get("actions")) {
                    assertThat(AuditActionCatalog.byCode()).as("registry action unknown: %s", action.asText())
                            .containsKey(action.asText());
                }
            } else {
                assertThat(entry.has("excluded") && entry.get("excluded").asText().isBlank()).isFalse();
            }
        }

        Set<String> liveRoutes = liveMutatingRoutes();
        for (String live : liveRoutes) {
            assertThat(byRoute).as("mutating route without coverage entry: %s", live).containsKey(live);
        }
        for (String registered : byRoute.keySet()) {
            assertThat(liveRoutes).as("stale registry route (no such endpoint): %s", registered)
                    .contains(registered);
        }
    }

    /**
     * Resolves a referenced test class to its source file under
     * {@code backend/<module>/src/test/java}. A renamed, moved or deleted
     * test fails the gate; cross-module references work because the check
     * is source-based, not classpath-based (test classes are not shared
     * across modules).
     */
    private void assertThatTestSourceExists(JsonNode test) {
        String className = test.get("class").asText();
        String module = test.get("module").asText();
        assertThat(module).isIn("common", "icu-chart", "medication-sheet", "prosthesis-manufacturing");
        java.nio.file.Path source = backendRoot()
                .resolve(module).resolve("src/test/java")
                .resolve(className.replace('.', '/') + ".java");
        assertThat(java.nio.file.Files.isRegularFile(source))
                .as("coverage test source missing: %s", source).isTrue();
    }

    private java.nio.file.Path backendRoot() {
        String userDir = System.getProperty("user.dir", "");
        java.nio.file.Path dir = java.nio.file.Paths.get(userDir);
        if (dir.getFileName() != null && dir.getFileName().toString().equals("icu-chart")) {
            return dir.getParent();
        }
        try {
            var resource = getClass().getResource("/audit-action-coverage.json");
            if (resource != null && "file".equals(resource.getProtocol())) {
                // .../backend/<module>/target/test-classes/audit-action-coverage.json
                java.nio.file.Path testClasses = java.nio.file.Paths.get(resource.toURI());
                java.nio.file.Path moduleDir = testClasses.getParent().getParent().getParent();
                if (moduleDir.getFileName().toString().endsWith("chart")
                        || moduleDir.getFileName().toString().equals("common")) {
                    return moduleDir.getParent();
                }
            }
        } catch (Exception fallback) {
            // fall through to user.dir below
        }
        return dir;
    }

    private Set<String> liveMutatingRoutes() {
        Set<String> routes = new HashSet<>();
        for (Map.Entry<RequestMappingInfo, org.springframework.web.method.HandlerMethod> entry :
                handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            if (info.getMethodsCondition().getMethods().isEmpty()) {
                continue;
            }
            String handler = entry.getValue().getBeanType().getName();
            if (handler.contains(".test.") || handler.contains("Fixture") || handler.contains("Test")) {
                continue;
            }
            var patterns = info.getPatternsCondition() != null
                    ? info.getPatternsCondition().getPatterns()
                    : Set.<String>of();
            var pathPatterns = info.getPathPatternsCondition() != null
                    ? info.getPathPatternsCondition().getPatterns()
                    : Set.<org.springframework.web.util.pattern.PathPattern>of();
            Set<String> paths = new HashSet<>(patterns);
            pathPatterns.forEach(pattern -> paths.add(pattern.getPatternString()));
            for (RequestMethod method : info.getMethodsCondition().getMethods()) {
                if (method != RequestMethod.POST && method != RequestMethod.PUT
                        && method != RequestMethod.PATCH && method != RequestMethod.DELETE) {
                    continue;
                }
                for (String path : paths) {
                    if (path.startsWith("/api/")) {
                        routes.add(method.name() + " " + path);
                    }
                }
            }
        }
        return routes;
    }
}
