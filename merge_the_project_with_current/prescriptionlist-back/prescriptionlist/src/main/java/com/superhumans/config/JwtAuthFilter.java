package com.superhumans.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtAuthFilter extends OncePerRequestFilter {

    UserAuthenticationProvider userAuthenticationProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                if ("GET".equals(request.getMethod())) {
                    SecurityContextHolder.getContext().setAuthentication(
                            userAuthenticationProvider.validateToken(token)
                    );
                } else {
                    SecurityContextHolder.getContext().setAuthentication(
                            userAuthenticationProvider.validateTokenStrongly(token)
                    );
                }
            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext();
                throw ex;
            }
        }

        filterChain.doFilter(request, response);
    }
}
