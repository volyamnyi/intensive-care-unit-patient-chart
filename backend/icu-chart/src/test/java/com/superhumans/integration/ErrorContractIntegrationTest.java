package com.superhumans.integration;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for the framework-4xx handlers added in CR-05 (#401):
 * param-level validation on {@code @Validated} controllers and malformed
 * bodies must return 400 — not the misleading 500 they produced when they
 * fell through to the RuntimeException catch-all.
 */
class ErrorContractIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void paramValidation_returns400() {
        // ProstheticsOrderController has @Validated + @Pattern("\\d+") on
        // patientId — "abc" triggers HandlerMethodValidationException.
        // Pre-fix: 500 INTERNAL_ERROR; post-fix: 400 VALIDATION_ERROR.
        String token = prosthetistToken();
        var res = restTemplate.exchange(
                "/api/prosthesis-manufacturing/orders?patientId=abc",
                HttpMethod.GET, authGet(token), String.class);

        assertThat(res.getStatusCode())
                .as("param validation on @Validated controllers must be 400, not 500")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).contains("VALIDATION_ERROR");
    }

    @Test
    void unreadableBody_returns400() {
        // Malformed JSON body on any @RequestBody endpoint.
        var res = restTemplate.exchange(
                "/api/episodes", HttpMethod.POST,
                authEntity("{\"patientId\":", getDoctorToken()), String.class);

        assertThat(res.getStatusCode())
                .as("malformed JSON body must be 400, not 500")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).contains("VALIDATION_ERROR");
    }

    private String prosthetistToken() {
        User user = userRepository.findByLogin("prosthetist-error-contract")
                .orElseGet(() -> userRepository.save(User.builder()
                        .login("prosthetist-error-contract")
                        .passwordHash("not-used")
                        .fullName("Prosthetist ErrorContract")
                        .role(UserRole.PROSTHETIST)
                        .build()));
        return jwtTokenProvider.generateToken(user.getLogin(),
                UserRole.PROSTHETIST.name(), user.getId());
    }
}
