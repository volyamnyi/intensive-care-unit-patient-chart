package com.superhumans.auth;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditClientIpResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventRecorder;
import com.superhumans.repository.core.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    JwtTokenProvider jwtTokenProvider;
    ObjectProvider<AuditEventRecorder> auditEventRecorderProvider;
    ObjectProvider<AuditClientIpResolver> clientIpResolverProvider;
    ObjectProvider<UserRepository> userRepositoryProvider;
    ObjectProvider<TokenRevocationService> tokenRevocationServiceProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        TokenRevocationService tokenRevocationService = tokenRevocationServiceProvider.getIfAvailable();
        // Single parse per request (#371 item 4): the per-getter re-parses
        // cost up to 5 HMAC verifications on the hot path.
        io.jsonwebtoken.Claims claims = null;
        if (token != null) {
            try {
                claims = jwtTokenProvider.getClaims(token);
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                claims = null;
            }
        }
        if (claims != null
                && (tokenRevocationService == null
                || !tokenRevocationService.isRevoked(claims.getId()))) {
            String login = claims.getSubject();
            String role = claims.get("role", String.class);
            String userIdRaw = claims.get("userId", String.class);
            Long userId = userIdRaw != null ? Long.valueOf(userIdRaw) : null;
            UserRepository userRepository = userRepositoryProvider.getIfAvailable();
            if (userRepository != null) {
                try {
                    var currentUser = userId == null ? null
                            : userRepository.findById(userId).orElse(null);
                    if (currentUser == null || Boolean.TRUE.equals(currentUser.getDeleted())
                            || !currentUser.getRole().name().equals(role)) {
                        recordTokenRejected(request, "AUTH_TOKEN_REJECTED");
                        filterChain.doFilter(request, response);
                        return;
                    }
                } catch (org.springframework.dao.DataAccessException userStoreUnavailable) {
                    // #367: a core-DB outage must degrade every authenticated
                    // request to the same uniform fail-closed 401, not a
                    // container 500 from the filter. The token is valid, but the
                    // live-user re-check cannot run, so the request continues
                    // unauthenticated and the security chain rejects it.
                    log.warn("User re-check store unavailable for token subject {}: {}",
                            login, userStoreUnavailable.getMessage());
                    recordTokenRejected(request, "AUTH_USER_STORE_UNAVAILABLE");
                    filterChain.doFilter(request, response);
                    return;
                }
            }
            var auth = new UsernamePasswordAuthenticationToken(
                    login, userId,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } else if (token != null) {
            recordTokenRejected(request, "AUTH_TOKEN_REJECTED");
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Records presenting an invalid, revoked or stale token. Only non-GET requests are
     * recorded to avoid a write per idle-polling request with an expired session; the
     * outcome is always DENIED and the token value itself is never stored.
     *
     * <p>Best-effort (#367): during a core-DB outage the audit store is usually
     * dead too, and the filter must never propagate - the uniform 401 matters
     * more than the audit row.
     */
    private void recordTokenRejected(HttpServletRequest request, String errorCode) {
        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return;
        }
        AuditEventRecorder recorder = auditEventRecorderProvider.getIfAvailable();
        if (recorder == null) {
            return;
        }
        AuditClientIpResolver resolver = clientIpResolverProvider.getIfAvailable();
        try {
            recorder.record(AuditEvent.builder()
                    .actor(AuditActorResolver.unknown())
                    .eventClass(EventClass.SECURITY)
                    .module("platform")
                    .functionalArea("token")
                    .action("platform.auth.token.rejected")
                    .actionType(ActionType.ACCESS_DENIED)
                    .outcome(AuditEvent.AuditOutcome.DENIED)
                    .errorCode(errorCode)
                    .ipAddress(resolver == null ? null : resolver.resolveClientIp(request))
                    .userAgentClass(AuditClientIpResolver.reduceUserAgent(request.getHeader("User-Agent")))
                    .source(AuditEvent.AuditSource.API)
                    .build());
        } catch (RuntimeException auditStoreAlsoDown) {
            log.debug("Token-rejection audit unavailable: {}", auditStoreAlsoDown.getMessage());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
