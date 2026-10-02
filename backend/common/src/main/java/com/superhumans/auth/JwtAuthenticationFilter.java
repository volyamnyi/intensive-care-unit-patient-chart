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
        if (token != null && jwtTokenProvider.validateToken(token)
                && (tokenRevocationService == null
                || !tokenRevocationService.isRevoked(jwtTokenProvider.getJtiFromToken(token)))) {
            String login = jwtTokenProvider.getLoginFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            UserRepository userRepository = userRepositoryProvider.getIfAvailable();
            if (userRepository != null) {
                var currentUser = userId == null ? null : userRepository.findById(userId).orElse(null);
                if (currentUser == null || Boolean.TRUE.equals(currentUser.getDeleted())
                        || !currentUser.getRole().name().equals(role)) {
                    recordTokenRejected(request);
                    filterChain.doFilter(request, response);
                    return;
                }
            }
            var auth = new UsernamePasswordAuthenticationToken(
                    login, userId,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } else if (token != null) {
            recordTokenRejected(request);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Records presenting an invalid, revoked or stale token. Only non-GET requests are
     * recorded to avoid a write per idle-polling request with an expired session; the
     * outcome is always DENIED and the token value itself is never stored.
     */
    private void recordTokenRejected(HttpServletRequest request) {
        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return;
        }
        AuditEventRecorder recorder = auditEventRecorderProvider.getIfAvailable();
        if (recorder == null) {
            return;
        }
        AuditClientIpResolver resolver = clientIpResolverProvider.getIfAvailable();
        recorder.record(AuditEvent.builder()
                .actor(AuditActorResolver.unknown())
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("token")
                .action("platform.auth.token.rejected")
                .actionType(ActionType.ACCESS_DENIED)
                .outcome(AuditEvent.AuditOutcome.DENIED)
                .errorCode("AUTH_TOKEN_REJECTED")
                .ipAddress(resolver == null ? null : resolver.resolveClientIp(request))
                .userAgentClass(AuditClientIpResolver.reduceUserAgent(request.getHeader("User-Agent")))
                .source(AuditEvent.AuditSource.API)
                .build());
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
