package com.superhumans.service;

import com.superhumans.entity.core.AuthProvider;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Seeds application user accounts from environment variables instead of SQL,
 * so no user values — and no password material — ever live in seed scripts.
 *
 * <p>Contract (names only; values come from the shell environment):
 * {@code APP_TEST_USERNAME1..9} and {@code APP_TEST_PASSWORD1..9} are
 * required per account; {@code APP_TEST_USERROLE1..9},
 * {@code APP_TEST_USERFULLNAME[1..9]}, {@code APP_TEST_EMAIL1..9},
 * {@code APP_TEST_PHONE1..9} and {@code APP_TEST_PROFESSION1..9} are
 * optional metadata. A missing pair skips that account with a warning.
 * Existing logins are never overwritten, so operator-rotated credentials
 * survive restarts (audit finding A2). All accounts are {@code LOCAL};
 * directory users provision themselves on first bind (decision D1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSeedService {

    static final int SEED_USER_COUNT = 9;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Seeds accounts from the process environment.
     */
    public void seedFromEnvironment() {
        seedFromEnvironment(System::getenv);
    }

    /**
     * Seeds accounts using the given environment lookup (test seam).
     *
     * @param env variable lookup by exact name, may return {@code null}
     */
    void seedFromEnvironment(Function<String, String> env) {
        int seeded = 0;
        for (int i = 1; i <= SEED_USER_COUNT; i++) {
            if (seedUser(env, i)) {
                seeded++;
            }
        }
        log.info("Seeded {} local application users from environment", seeded);
    }

    private boolean seedUser(Function<String, String> env, int index) {
        String suffix = String.valueOf(index);
        String login = env.apply("APP_TEST_USERNAME" + suffix);
        String password = env.apply("APP_TEST_PASSWORD" + suffix);
        if (isBlank(login) || password == null || password.isEmpty()) {
            log.warn("Skipping seed user {}: APP_TEST_USERNAME{}/APP_TEST_PASSWORD{} not configured",
                    index, suffix, suffix);
            return false;
        }
        if (userRepository.findByLogin(login).isPresent()) {
            return false;
        }
        UserRole role = parseRole(env.apply("APP_TEST_USERROLE" + suffix), suffix);
        User user = User.builder()
                .login(login)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(orElse(metadata(env, "APP_TEST_USERFULLNAME", index), login))
                .role(role)
                .authProvider(AuthProvider.LOCAL)
                .email(emptyToNull(env.apply("APP_TEST_EMAIL" + index)))
                .phone(emptyToNull(env.apply("APP_TEST_PHONE" + index)))
                .specialityName(emptyToNull(metadata(env, "APP_TEST_PROFESSION", index)))
                .build();
        userRepository.save(user);
        return true;
    }

    private static UserRole parseRole(String value, String suffix) {
        if (value == null || value.isBlank()) {
            log.warn("APP_TEST_USERROLE{} not configured, seeding GUEST (no permissions)", suffix);
            return UserRole.GUEST;
        }
        try {
            return UserRole.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid APP_TEST_USERROLE{} value, seeding GUEST (no permissions)", suffix);
            return UserRole.GUEST;
        }
    }

    private static String metadata(Function<String, String> env, String base, int index) {
        String suffixed = env.apply(base + index);
        if (suffixed != null) {
            return suffixed;
        }
        return index == 1 ? env.apply(base) : null;
    }

    private static String orElse(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
