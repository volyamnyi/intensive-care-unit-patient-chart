package com.superhumans.integration;

import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.core.AuthProvider;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.integration.ldap.EmbeddedLdapServer;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.repository.core.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration coverage for the directory authentication path against a
 * hermetic in-memory directory (issue #249): the real
 * {@code LdapAuthenticationProvider}, bind, search, mapping, provisioning,
 * JWT issuance, and failure semantics — without touching corporate AD.
 *
 * <p>Local-only suite: the corporate directory is unreachable from CI
 * runners. Test identities seed exclusively from environment variables
 * ({@code APP_TEST_USERNAME1..9} / {@code APP_TEST_PASSWORD1..9} plus
 * optional profile metadata); role variables are never consumed because
 * authorization stays with the local database (decision D4). At least the
 * first identity pair must be present, otherwise the class aborts.
 */
@EnabledIfSystemProperty(named = "ldap.local.tests", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LdapDirectoryIntegrationTest extends AbstractIntegrationTest {

    private static EmbeddedLdapServer directory;

    @DynamicPropertySource
    static void ldapProperties(DynamicPropertyRegistry registry) {
        directory = EmbeddedLdapServer.start();
        registry.add("app.ldap.enabled", () -> "true");
        registry.add("app.ldap.urls", () -> "ldap://localhost:" + directory.port());
        registry.add("app.ldap.base", () -> EmbeddedLdapServer.BASE_DN);
        registry.add("app.ldap.username", () -> EmbeddedLdapServer.SERVICE_DN);
        registry.add("app.ldap.password", directory::servicePassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeAll
    void requireFreshTestIdentities() {
        Assumptions.assumeTrue(!directory.seededLogins().isEmpty(),
                "Set APP_TEST_USERNAME1/APP_TEST_PASSWORD1 (optionally 2..9) to run local LDAP tests");
        for (String login : directory.seededLogins()) {
            Assumptions.assumeTrue(userRepository.findByLogin(login).isEmpty(),
                    "Test identity already provisioned, use fresh logins: " + login);
        }
    }

    @AfterAll
    void cleanUpAndStop() {
        for (String login : directory.seededLogins()) {
            userRepository.findByLogin(login).ifPresent(userRepository::delete);
        }
        directory.stop();
    }

    @Test
    @Order(1)
    void invalidPasswordOnFreshIdentity_returns401WithoutRowAndAudits() {
        String login = directory.seededLogins().get(0);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(login, "definitely-wrong"), LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
        assertThat(userRepository.findByLogin(login)).isEmpty();
        assertThat(auditDetails("LOGIN_FAILED")).anyMatch(details -> details.contains(login));
    }

    @Test
    @Order(2)
    void firstLogin_provisionsGuestIssuesJwtAndAudits() {
        String login = directory.seededLogins().get(0);
        String password = directoryPassword(login);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(login, password), LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRole()).isEqualTo("GUEST");
        String cookie = extractJwtCookie(response);
        assertThat(cookie).isNotBlank();

        User stored = userRepository.findByLogin(login).orElseThrow();
        assertThat(stored.getRole()).isEqualTo(UserRole.GUEST);
        assertThat(stored.getAuthProvider()).isEqualTo(AuthProvider.LDAP);
        assertThat(stored.getPasswordHash()).isNull();
        assertThat(stored.getFullName()).isNotBlank();

        ResponseEntity<User> me =
                restTemplate.exchange("/api/users/me", HttpMethod.GET, cookieGet(cookie), User.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().getRole()).isEqualTo(UserRole.GUEST);

        ResponseEntity<String[]> permissions = restTemplate.exchange("/api/users/me/permissions",
                HttpMethod.GET, cookieGet(cookie), String[].class);
        assertThat(permissions.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(permissions.getBody()).isEmpty();

        ResponseEntity<String> audit = restTemplate.exchange("/api/audit",
                HttpMethod.GET, cookieGet(cookie), String.class);
        assertThat(audit.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(auditDetails("LOGIN")).anyMatch(details -> details.contains(login));
    }

    @Test
    @Order(3)
    void everyFreshIdentity_firstLoginIsGuest() {
        for (String login : directory.seededLogins()) {
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                    "/api/auth/login", new LoginRequest(login, directoryPassword(login)),
                    LoginResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getRole()).isEqualTo("GUEST");
        }
    }

    @Test
    @Order(4)
    void repeatedFailures_throttleWithTooManyRequests() {
        // Dedicated identity: its lockout must not leak into other tests, so at
        // least two seeded pairs are required for this scenario.
        Assumptions.assumeTrue(directory.seededLogins().size() >= 2,
                "Need APP_TEST_USERNAME2/PASSWORD2 for isolated throttle coverage");
        // Lockout windows grow exponentially (1s, 2s, 4s, ...), so consecutive
        // attempts deterministically observe a blocked one without any sleeps.
        String login = directory.seededLogins().get(directory.seededLogins().size() - 1);
        boolean blocked = false;
        for (int i = 0; i < 10 && !blocked; i++) {
            int code = restTemplate.postForEntity("/api/auth/login",
                    new LoginRequest(login, "definitely-wrong"), LoginResponse.class)
                    .getStatusCode().value();
            assertThat(code).isIn(401, 429);
            blocked = code == 429;
        }
        assertThat(blocked).isTrue();
    }

    @Test
    @Order(5)
    void promotedIdentity_reLoginKeepsRoleAndGrants() {
        String login = directory.seededLogins().get(0);
        Long userId = loginUserId(login, directoryPassword(login));
        String adminCookie = loginAs("admin", "admin123");

        HttpEntity<Map<String, String>> promote = authEntity(Map.of("role", "DOCTOR"), adminCookie);
        ResponseEntity<User> promoted = restTemplate.exchange(
                "/api/admin/users/" + userId + "/role", HttpMethod.PUT, promote, User.class);
        assertThat(promoted.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<LoginResponse> relogin = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(login, directoryPassword(login)),
                LoginResponse.class);

        assertThat(relogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(relogin.getBody().getRole()).isEqualTo("DOCTOR");
        assertThat(userRepository.findByLogin(login).orElseThrow().getAuthProvider())
                .isEqualTo(AuthProvider.LDAP);

        ResponseEntity<String[]> permissions = restTemplate.exchange("/api/users/me/permissions",
                HttpMethod.GET, cookieGet(extractJwtCookie(relogin)), String[].class);
        assertThat(permissions.getBody()).contains("EPISODE_CREATE");
    }

    @Test
    @Order(6)
    void logout_revokesDirectoryIssuedSession() {
        String login = directory.seededLogins().get(0);
        String cookie = loginAs(login, directoryPassword(login));
        assertThat(cookie).isNotNull();

        ResponseEntity<Void> logout = restTemplate.exchange("/api/auth/logout",
                HttpMethod.POST, cookieGet(cookie), Void.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> reuse = restTemplate.exchange("/api/users/me",
                HttpMethod.GET, cookieGet(cookie), String.class);
        assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(7)
    void directoryOutage_returns401WithoutHanging() {
        directory.stop();
        String login = directory.seededLogins().get(0);

        long started = System.currentTimeMillis();
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(login, directoryPassword(login)),
                LoginResponse.class);
        long elapsed = System.currentTimeMillis() - started;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
        assertThat(elapsed).isLessThan(60_000L);
    }

    private String directoryPassword(String login) {
        String password = directory.passwordFor(login);
        assertThat(password)
                .as("Seeded identity %s must have a password", login)
                .isNotNull();
        return password;
    }

    private List<String> auditDetails(String action) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action, Pageable.unpaged())
                .getContent().stream()
                .map(log -> log.getDetails() == null ? "" : log.getDetails())
                .toList();
    }
}
