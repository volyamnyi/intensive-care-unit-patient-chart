package com.superhumans.auth;

import com.superhumans.entity.AuditLog;
import com.superhumans.repository.AuditLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogRepository auditLogRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String login = jwtTokenProvider.getLoginFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);
            var auth = new UsernamePasswordAuthenticationToken(
                    login, jwtTokenProvider.getUserIdFromToken(token),
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(auth);

            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(jwtTokenProvider.getUserIdFromToken(token));
            auditLog.setEntity("AUTH");
            auditLog.setAction("LOGIN");
            auditLog.setDetails("User logged in: " + login);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setIpAddress(request.getRemoteAddr());
            auditLog.setUserRole(role);
            auditLogRepository.save(auditLog);
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
