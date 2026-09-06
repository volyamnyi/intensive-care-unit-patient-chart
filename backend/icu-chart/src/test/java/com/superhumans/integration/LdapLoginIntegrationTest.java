package com.superhumans.integration;

import com.superhumans.auth.LdapAuthService;
import com.superhumans.auth.LdapUserProfile;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.core.AuthProvider;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * End-to-end backend coverage for the directory authentication path
 * (issue #246, decision D1): {@code POST /api/auth/login} with a mocked
 * directory provider against the real HTTP stack and database.
 *
 * <p>Local-only suite: the corporate directory is unreachable from CI
 * runners. The provider is mocked here (no directory I/O at all); live
 * directory coverage arrives with the test double in issue #249.
 */
@EnabledIfSystemProperty(named = "ldap.local.tests", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LdapLoginIntegrationTest extends AbstractIntegrationTest {

    private static final String GUEST_LOGIN = "ad.newbie";
    private static final String GUEST_PASSWORD = "Secret123";

    @MockitoBean
    private LdapAuthService ldapAuthService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUpProvisionedUsers() {
        userRepository.findByLogin(GUEST_LOGIN).ifPresent(userRepository::delete);
        userRepository.findByLogin("ad.bad").ifPresent(userRepository::delete);
    }

    @Test
    @Order(1)
    void ldapFirstLogin_provisionsGuestIssuesJwtAndBlocksProtectedReads() {
        when(ldapAuthService.authenticate(GUEST_LOGIN, GUEST_PASSWORD)).thenReturn(Optional.of(
                new LdapUserProfile(GUEST_LOGIN, "Ad Newbie", "ad@hospital.ua",
                        "380500000000", "Doctor")));

        ResponseEntity<LoginResponse> login = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(GUEST_LOGIN, GUEST_PASSWORD),
                LoginResponse.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();
        assertThat(login.getBody().getRole()).isEqualTo("GUEST");
        String cookie = extractJwtCookie(login);
        assertThat(cookie).isNotBlank();

        User stored = userRepository.findByLogin(GUEST_LOGIN).orElseThrow();
        assertThat(stored.getRole()).isEqualTo(UserRole.GUEST);
        assertThat(stored.getAuthProvider()).isEqualTo(AuthProvider.LDAP);
        assertThat(stored.getPasswordHash()).isNull();
        assertThat(stored.getFullName()).isEqualTo("Ad Newbie");

        ResponseEntity<User> me = restTemplate.exchange("/api/users/me",
                HttpMethod.GET, cookieGet(cookie), User.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().getRole()).isEqualTo(UserRole.GUEST);

        ResponseEntity<String[]> permissions = restTemplate.exchange("/api/users/me/permissions",
                HttpMethod.GET, cookieGet(cookie), String[].class);
        assertThat(permissions.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(permissions.getBody()).isEmpty();

        ResponseEntity<String> audit = restTemplate.exchange("/api/audit",
                HttpMethod.GET, cookieGet(cookie), String.class);
        assertThat(audit.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(2)
    void ldapReLogin_preservesPromotedRole() {
        when(ldapAuthService.authenticate(GUEST_LOGIN, GUEST_PASSWORD)).thenReturn(Optional.of(
                new LdapUserProfile(GUEST_LOGIN, "Ad Newbie", null, null, null)));
        String guestCookie = loginAs(GUEST_LOGIN, GUEST_PASSWORD);
        assertThat(guestCookie).isNotNull();
        Long guestId = loginUserId(GUEST_LOGIN, GUEST_PASSWORD);

        String adminCookie = loginAs("admin", "admin123");
        HttpEntity<Map<String, String>> promote =
                authEntity(Map.of("role", "DOCTOR"), adminCookie);
        ResponseEntity<User> promoted = restTemplate.exchange(
                "/api/admin/users/" + guestId + "/role", HttpMethod.PUT, promote, User.class);
        assertThat(promoted.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<LoginResponse> relogin = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(GUEST_LOGIN, GUEST_PASSWORD),
                LoginResponse.class);

        assertThat(relogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(relogin.getBody().getRole()).isEqualTo("DOCTOR");
        assertThat(userRepository.findByLogin(GUEST_LOGIN).orElseThrow().getAuthProvider())
                .isEqualTo(AuthProvider.LDAP);

        ResponseEntity<String[]> permissions = restTemplate.exchange("/api/users/me/permissions",
                HttpMethod.GET, cookieGet(extractJwtCookie(relogin)), String[].class);
        assertThat(permissions.getBody()).contains("EPISODE_CREATE");
    }

    @Test
    @Order(3)
    void ldapBindFailure_returnsUnauthorizedWithoutRow() {
        when(ldapAuthService.authenticate("ad.bad", "wrong")).thenReturn(Optional.empty());

        ResponseEntity<LoginResponse> login = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest("ad.bad", "wrong"), LoginResponse.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login.getBody()).isNull();
        assertThat(userRepository.findByLogin("ad.bad")).isEmpty();
    }

    @Test
    @Order(4)
    void localLogin_regressionAfterD1Branch() {
        assertThat(loginAs("doctor1", "doctor123")).isNotNull();
        assertThat(loginAs("nurse1", "nurse123")).isNotNull();
    }
}
