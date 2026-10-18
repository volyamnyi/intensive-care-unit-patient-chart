package com.superhumans.service;

import com.superhumans.auth.LdapAuthService;
import com.superhumans.auth.LdapUserProfile;
import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventRecorder;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.core.AuthProvider;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

private static final int MAX_FAILURES_BEFORE_LOCKOUT = 5;
    private static final long MAX_LOCKOUT_SECONDS = 60;
    /**
     * #361: per-IP aggregate window. The per-key limiter (ip|login, 5 failures)
     * never fires for password spraying - one failure per login. This caps the
     * total failed logins from one IP inside a rolling minute, so spraying is
     * throttled even when every guess uses a fresh login name. 100/min is far
     * above anything a NAT-shared clinical floor produces in legitimate
     * failures, turns unbounded line-rate guessing into a bounded rate, and
     * stays clear of the integration suites' own probe volume.
     */
    private static final int MAX_FAILURES_PER_IP_PER_MINUTE = 100;
    /**
     * #361: how long a per-key counter survives its last failure. Long enough
     * that waiting out the 60s lockout does not reset the failure count, short
     * enough that the map cannot grow without bound.
     */
    private static final long ATTEMPT_RETENTION_SECONDS = 900;
    // Valid BCrypt hash used to keep the unknown-user path comparable to a real
    // password check. Package-visible so the timing tests can pin the padding.
    static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    AuditService auditService;
    AuditEventRecorder auditEventRecorder;
    ObjectProvider<LdapAuthService> ldapAuthServiceProvider;
    Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    /** #361: rolling per-IP failure window, mirrors the per-key limiter. */
    final Map<String, Deque<Instant>> ipFailures = new ConcurrentHashMap<>();

    public ResponseEntity<LoginResponse> login(LoginRequest req) {
        return login(req, null);
    }

    public ResponseEntity<LoginResponse> login(LoginRequest req, String ipAddress) {
        sweepExpiredAttempts();
        if (isIpThrottled(ipAddress)) {
            auditService.logAuth("LOGIN_BLOCKED_IP", null, null, ipAddress,
                    "Too many failed logins from this address within a minute: " + ipAddress);
            auditEventRecorder.record(AuditEvent.builder()
                    .actor(attemptedLoginActor(req.getLogin()))
                    .eventClass(EventClass.SECURITY)
                    .module("platform")
                    .functionalArea("session")
                    .action("platform.auth.session.login.blocked")
                    .actionType(ActionType.AUTHENTICATE)
                    .outcome(AuditEvent.AuditOutcome.DENIED)
                    .errorCode("AUTH_TOO_MANY_IP_FAILURES")
                    .ipAddress(ipAddress)
                    .source(AuditEvent.AuditSource.API)
                    .build());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(null);
        }
        String attemptKey = (ipAddress == null ? "unknown" : ipAddress) + "|" + req.getLogin();
        LoginAttempt attempt = loginAttempts.computeIfAbsent(attemptKey, key -> new LoginAttempt());
        if (!attempt.isAllowed()) {
            auditService.logAuth("LOGIN_BLOCKED", null, null, ipAddress,
                    "Login temporarily blocked for login: " + req.getLogin());
            auditEventRecorder.record(AuditEvent.builder()
                    .actor(attemptedLoginActor(req.getLogin()))
                    .eventClass(EventClass.SECURITY)
                    .module("platform")
                    .functionalArea("session")
                    .action("platform.auth.session.login.blocked")
                    .actionType(ActionType.AUTHENTICATE)
                    .outcome(AuditEvent.AuditOutcome.DENIED)
                    .errorCode("AUTH_RATE_LIMITED")
                    .ipAddress(ipAddress)
                    .source(AuditEvent.AuditSource.API)
                    .build());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(null);
        }

        User user = userRepository.findByLogin(req.getLogin()).orElse(null);

        LdapAuthService ldap = ldapService();
        if (user == null && ldap != null) {
            return loginUnknownWithLdap(req, ipAddress, attemptKey, attempt, ldap);
        }
        if (user != null && user.getAuthProvider() == AuthProvider.LDAP && ldap != null) {
            return loginLdapUser(req, ipAddress, attemptKey, attempt, ldap, user);
        }
        // LOCAL accounts, unknown logins without directory integration, and LDAP-marked
        // accounts while the integration is disabled all take the BCrypt path with the
        // exact legacy semantics (a NULL hash never matches, it only fails closed).
        // #366: a NULL hash must still PAY the BCrypt cost - matches(raw, null)
        // short-circuits without hashing, and that timing classifies LDAP rows.
        String storedHash = user == null ? DUMMY_PASSWORD_HASH : user.getPasswordHash();
        boolean nullHash = storedHash == null;
        boolean passwordMatches;
        if (nullHash) {
            payBcryptCost(req.getPassword());
            passwordMatches = false;
        } else {
            // The match always runs (unknown logins pay the dummy cost); the
            // result only counts when there is a real account to log into.
            passwordMatches = passwordEncoder.matches(req.getPassword(), storedHash)
                    && user != null;
        }
        if (user == null || nullHash || !passwordMatches) {
            return loginFailed(req, ipAddress, attempt, user);
        }
        // #362: a disabled (soft-deleted) account must not authenticate. The HTTP
        // response stays the generic 401 so the caller cannot tell a wrong password
        // from a disabled account; only the audit row records the difference.
        if (Boolean.TRUE.equals(user.getDeleted())) {
            return loginFailed(req, ipAddress, attempt, user, "AUTH_ACCOUNT_DISABLED");
        }

        return loginSucceeded(ipAddress, attemptKey, user);
    }

    public void logout(Long userId, String userRole, String ipAddress) {
        auditService.logAuth("LOGOUT", userId, userRole, ipAddress, "User logged out");
        auditEventRecorder.record(AuditEvent.builder()
                .actor(new AuditEvent.AuditActor(
                        AuditEvent.ActorType.USER,
                        userId == null ? null : userId.toString(),
                        null,
                        null,
                        userRole == null ? Set.of() : Set.of(userRole),
                        null,
                        null))
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("session")
                .action("platform.auth.session.logout")
                .actionType(ActionType.LOGOUT)
                .target(userId == null ? null
                        : new AuditEvent.AuditTarget("User", userId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .ipAddress(ipAddress)
                .source(AuditEvent.AuditSource.API)
                .build());
    }

    private LdapAuthService ldapService() {
        return ldapAuthServiceProvider != null ? ldapAuthServiceProvider.getIfAvailable() : null;
    }

    private ResponseEntity<LoginResponse> loginLdapUser(LoginRequest req, String ipAddress,
            String attemptKey, LoginAttempt attempt, LdapAuthService ldap, User user) {
        Optional<LdapUserProfile> profile = ldap.authenticate(req.getLogin(), req.getPassword());
        if (profile.isEmpty()) {
            // #366: the LOCAL branch pays full BCrypt on failure; the bind path
            // must pay the same cost so response timing cannot classify the row.
            payBcryptCost(req.getPassword());
            return loginFailed(req, ipAddress, attempt, user);
        }
        // #362: a disabled account does not come back to life through the
        // directory branch either - the bind succeeded, the row is still dead.
        if (Boolean.TRUE.equals(user.getDeleted())) {
            payBcryptCost(req.getPassword());
            return loginFailed(req, ipAddress, attempt, user, "AUTH_ACCOUNT_DISABLED");
        }
        // The stored local role stays authoritative: a successful bind never
        // rewrites role, permissions, or profile fields (decision D1).
        return loginSucceeded(ipAddress, attemptKey, user);
    }

    private ResponseEntity<LoginResponse> loginUnknownWithLdap(LoginRequest req, String ipAddress,
            String attemptKey, LoginAttempt attempt, LdapAuthService ldap) {
        Optional<LdapUserProfile> profile = ldap.authenticate(req.getLogin(), req.getPassword());
        if (profile.isEmpty()) {
            // #366: same BCrypt cost as the LOCAL unknown-login failure.
            payBcryptCost(req.getPassword());
            return loginFailed(req, ipAddress, attempt, null);
        }
        User user = provisionLdapUser(profile.get());
        if (user == null) {
            payBcryptCost(req.getPassword());
            return loginFailed(req, ipAddress, attempt, null);
        }
        return loginSucceeded(ipAddress, attemptKey, user);
    }

    /**
     * #366: one discarded cost-10 BCrypt match so every login failure costs the
     * same. The dummy hash is a real hash, so this is full-price work; only the
     * result is thrown away. The directory bind's network latency stays an
     * accepted residual (runbook) - it is irreducible without slowing the
     * happy path deliberately.
     */
    private void payBcryptCost(String rawPassword) {
        passwordEncoder.matches(rawPassword, DUMMY_PASSWORD_HASH);
    }

    /**
     * Finds or creates the local account for a successfully bound directory identity.
     * The first login creates an {@code LDAP}/{@code GUEST} row with a NULL password
     * hash; a normalized-login collision with a LOCAL account fails closed with
     * {@code null} so directory identities can never inherit local roles.
     *
     * @param profile authenticated directory profile
     * @return attached local user, or {@code null} on LOCAL collision
     */
    private User provisionLdapUser(LdapUserProfile profile) {
        User existing = userRepository.findByLogin(profile.login()).orElse(null);
        if (existing != null) {
            if (existing.getAuthProvider() == AuthProvider.LDAP) {
                // #362: an admin-disabled identity must not be resurrected by a
                // directory bind - provisioning fails closed instead of returning
                // (and thereby re-enabling) the disabled row.
                if (Boolean.TRUE.equals(existing.getDeleted())) {
                    auditEventRecorder.record(provisionEvent(profile, null,
                            AuditEvent.AuditOutcome.FAILURE, "AUTH_ACCOUNT_DISABLED"));
                    return null;
                }
                return existing;
            }
            auditEventRecorder.record(provisionEvent(profile, null, AuditEvent.AuditOutcome.FAILURE,
                    "AUTH_LOCAL_COLLISION"));
            return null;
        }
        User fresh = User.builder()
                .login(profile.login())
                .passwordHash(null)
                .fullName(profile.fullName())
                .role(UserRole.GUEST)
                .authProvider(AuthProvider.LDAP)
                .email(profile.email())
                .phone(profile.phone())
                .specialityName(profile.specialityName())
                .build();
        try {
            User saved = userRepository.save(fresh);
            auditEventRecorder.record(provisionEvent(profile, saved, AuditEvent.AuditOutcome.SUCCESS, null));
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Concurrent first login won the race: reuse the winning row.
            User winner = userRepository.findByLogin(profile.login()).orElseThrow(() -> e);
            auditEventRecorder.record(provisionEvent(profile, winner, AuditEvent.AuditOutcome.SUCCESS, null));
            return winner;
        }
    }

    private AuditEvent provisionEvent(LdapUserProfile profile, User user,
            AuditEvent.AuditOutcome outcome, String errorCode) {
        return AuditEvent.builder()
                .actor(new AuditEvent.AuditActor(
                        AuditEvent.ActorType.SYSTEM,
                        "ldap-provisioning",
                        null,
                        null,
                        Set.of(),
                        new AuditEvent.AuditActor(
                                AuditEvent.ActorType.USER, null, profile.login(), profile.fullName(),
                                Set.of(), null, null),
                        null))
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("directory")
                .action("platform.auth.directory.provision")
                .actionType(ActionType.PROVISION)
                .target(user == null || user.getId() == null ? null
                        : new AuditEvent.AuditTarget("User", user.getId().toString(), null))
                .outcome(outcome)
                .errorCode(errorCode)
                .source(AuditEvent.AuditSource.INTEGRATION)
                .build();
    }

    private static AuditEvent.AuditActor attemptedLoginActor(String login) {
        return new AuditEvent.AuditActor(
                AuditEvent.ActorType.USER, null, login, null, Set.of(), null, null);
    }

    private ResponseEntity<LoginResponse> loginFailed(LoginRequest req, String ipAddress,
            LoginAttempt attempt, User user) {
        return loginFailed(req, ipAddress, attempt, user, "AUTH_INVALID_CREDENTIALS");
    }

    /**
     * #362: the HTTP contract is one generic 401 - the caller must not learn WHY
     * the attempt failed. The errorCode lives in the audit row only, so forensics
     * can tell a wrong password from a disabled account.
     */
    private ResponseEntity<LoginResponse> loginFailed(LoginRequest req, String ipAddress,
            LoginAttempt attempt, User user, String errorCode) {
        attempt.recordFailure();
        recordIpFailure(ipAddress);
        auditService.logAuth("LOGIN_FAILED",
                user == null ? null : user.getId(),
                user == null ? null : user.getRole().name(),
                ipAddress,
                "Failed login attempt for login: " + req.getLogin());
        auditEventRecorder.record(AuditEvent.builder()
                .actor(user == null ? attemptedLoginActor(req.getLogin()) : AuditActorResolver.fromUser(user))
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("session")
                .action("platform.auth.session.login.failed")
                .actionType(ActionType.AUTHENTICATE)
                .target(user == null || user.getId() == null ? null
                        : new AuditEvent.AuditTarget("User", user.getId().toString(), null))
                .outcome(AuditEvent.AuditOutcome.FAILURE)
                .errorCode(errorCode)
                .ipAddress(ipAddress)
                .source(AuditEvent.AuditSource.API)
                .build());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

    private ResponseEntity<LoginResponse> loginSucceeded(String ipAddress, String attemptKey,
            User user) {
        loginAttempts.remove(attemptKey);
        auditService.logAuth("LOGIN", user.getId(), user.getRole().name(), ipAddress,
                "Successful login for login: " + user.getLogin());
        auditEventRecorder.record(AuditEvent.builder()
                .actor(AuditActorResolver.fromUser(user))
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("session")
                .action("platform.auth.session.login")
                .actionType(ActionType.AUTHENTICATE)
                .target(user.getId() == null ? null
                        : new AuditEvent.AuditTarget("User", user.getId().toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .ipAddress(ipAddress)
                .source(AuditEvent.AuditSource.API)
                .build());

        return ResponseEntity.ok(LoginResponse.builder()
                .userId(user.getId())
                .login(user.getLogin())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .email(user.getEmail())
                .build());
    }

static final class LoginAttempt {
        private int failures;
        private Instant lockedUntil = Instant.MIN;
        private Instant lastFailureAt = Instant.MIN;

        synchronized boolean isAllowed() {
            return !Instant.now().isBefore(lockedUntil);
        }

        synchronized void recordFailure() {
            failures++;
            lastFailureAt = Instant.now();
            if (failures >= MAX_FAILURES_BEFORE_LOCKOUT) {
                long multiplier = 1L << Math.min(failures - MAX_FAILURES_BEFORE_LOCKOUT, 6);
                long seconds = Math.min(MAX_LOCKOUT_SECONDS, multiplier);
                lockedUntil = Instant.now().plus(Duration.ofSeconds(seconds));
            }
        }

        /** #361: swept once the lockout is over AND the counter has gone idle. */
        synchronized boolean isExpired() {
            return isAllowed()
                    && lastFailureAt != Instant.MIN
                    && Instant.now().isAfter(lastFailureAt.plus(Duration.ofSeconds(ATTEMPT_RETENTION_SECONDS)));
        }
    }

    /**
     * #361: the per-key map used to grow without bound - entries were removed only
     * on a successful login, so distinct keys (spoofed or sprayed) accumulated
     * forever. Expired lockouts and empty per-IP windows are swept on every
     * login attempt, so both structures stay proportional to live attackers.
     */
    void sweepExpiredAttempts() {
        loginAttempts.entrySet().removeIf(entry -> entry.getValue().isExpired());
        ipFailures.entrySet().removeIf(entry -> {
            pruneIpFailureWindow(entry.getValue());
            return entry.getValue().isEmpty();
        });
    }

    private boolean isIpThrottled(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        Deque<Instant> window = ipFailures.get(ipAddress);
        if (window == null) {
            return false;
        }
        pruneIpFailureWindow(window);
        return window.size() >= MAX_FAILURES_PER_IP_PER_MINUTE;
    }

    private void recordIpFailure(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        Deque<Instant> window = ipFailures.computeIfAbsent(
                ipAddress, key -> new java.util.concurrent.ConcurrentLinkedDeque<>());
        pruneIpFailureWindow(window);
        window.addLast(Instant.now());
    }

    private void pruneIpFailureWindow(Deque<Instant> window) {
        Instant cutoff = Instant.now().minus(Duration.ofSeconds(60));
        while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
            window.pollFirst();
        }
    }
}
