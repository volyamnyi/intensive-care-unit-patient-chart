package com.superhumans.auth;

import com.superhumans.entity.AuditLog;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    final JwtTokenProvider jwtTokenProvider;
    final AuditLogRepository auditLogRepository;
    final AuditService auditService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String login = jwtTokenProvider.getLoginFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            var auth = new UsernamePasswordAuthenticationToken(
                    login, userId,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(auth);

            String method = request.getMethod();
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                AuditLog auditLog = new AuditLog();
                auditLog.setUserId(userId);
                auditLog.setEntity(request.getRequestURI());
                String action = "API_" + method;
                auditLog.setAction(action);
                auditLog.setDetails("Authenticated " + action + " by: " + login);
                auditLog.setTimestamp(LocalDateTime.now());
                auditLog.setIpAddress(request.getRemoteAddr());
                auditLog.setUserRole(role);
                auditService.logAsync(auditLog);
            }
        }
        filterChain.doFilter(request, response);
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
