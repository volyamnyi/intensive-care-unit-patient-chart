package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.auth.LdapAuthService;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.core.AuthProvider;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.service.AuditService;
import com.superhumans.service.AuthService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

/**
 * Proves the CI authentication contract (issue #298): with
 * {@code app.ldap.enabled=false} no directory bean exists, LOCAL logins take
 * the BCrypt path, and the resulting JWT validates without any directory
 * involvement.
 *
 * <p>Unlike the LDAP suites (gated by {@code ldap.local.tests} and skipped in
 * CI), this class is deliberately ungated — it must run in CI, where the
 * directory is unreachable by design.
 */
class LdapDisabledInTestTest {

    /** 64-char ASCII secret: satisfies the HMAC-SHA key length without env. */
    private static final String TEST_JWT_SECRET =
            "test-only-jwt-secret-for-ldap-disabled-contract-0123456789abcdef";

    @Configuration
    static class TestDoubles {
        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return mock(PasswordEncoder.class);
        }

        @Bean
        AuditService auditService() {
            return mock(AuditService.class);
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(
                    TestDoubles.class, AuthService.class, LdapConfig.class, LdapAuthService.class)
            .withPropertyValues("app.ldap.enabled=false");

    @Test
    void disabled_createsNoDirectoryBeans() {
        runner.run(context -> assertThat(context)
                .doesNotHaveBean(LdapContextSource.class)
                .doesNotHaveBean(LdapAuthenticationProvider.class)
                .doesNotHaveBean(LdapAuthService.class)
                .hasSingleBean(AuthService.class));
    }

    @Test
    void disabled_localLoginIssuesJwtVerifiableWithoutDirectory() {
        runner.run(context -> {
            UserRepository users = context.getBean(UserRepository.class);
            PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
            when(users.findByLogin("doctor1")).thenReturn(Optional.of(localDoctor()));
            when(encoder.matches("doctor123", "encoded")).thenReturn(true);

            AuthService auth = context.getBean(AuthService.class);
            ResponseEntity<LoginResponse> response =
                    auth.login(new LoginRequest("doctor1", "doctor123"), "10.0.0.1");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getRole()).isEqualTo("DOCTOR");

            JwtTokenProvider jwt = new JwtTokenProvider(TEST_JWT_SECRET, 86400000L);
            String token = jwt.generateToken(response.getBody().getLogin(),
                    response.getBody().getRole(), response.getBody().getUserId());
            assertThat(jwt.validateToken(token)).isTrue();
            assertThat(jwt.getLoginFromToken(token)).isEqualTo("doctor1");
            assertThat(jwt.getRoleFromToken(token)).isEqualTo("DOCTOR");
            assertThat(jwt.getUserIdFromToken(token)).isEqualTo(11L);
        });
    }

    private static User localDoctor() {
        User user = User.builder()
                .login("doctor1")
                .passwordHash("encoded")
                .fullName("Test Doctor")
                .role(UserRole.DOCTOR)
                .authProvider(AuthProvider.LOCAL)
                .build();
        user.setId(11L);
        return user;
    }
}
