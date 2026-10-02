package com.superhumans.auth;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

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
                    filterChain.doFilter(request, response);
                    return;
                }
            }
            var auth = new UsernamePasswordAuthenticationToken(
                    login, userId,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(auth);

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
