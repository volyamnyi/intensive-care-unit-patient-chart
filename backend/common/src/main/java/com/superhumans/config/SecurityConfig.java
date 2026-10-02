package com.superhumans.config;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditClientIpResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventRecorder;
import com.superhumans.auth.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SecurityConfig {

    final JwtAuthenticationFilter jwtAuthFilter;
    final List<SecurityRuleContributor> ruleContributors;
    final AuditEventRecorder auditEventRecorder;
    final AuditClientIpResolver clientIpResolver;
    @Value("${server.ssl.enabled:false}")
    boolean sslEnabled;
    @Value("${springdoc.api-docs.enabled:true}")
    boolean apiDocsEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (sslEnabled) {
            http.redirectToHttps(Customizer.withDefaults());
        }
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        if (!isSafeMethod(request.getMethod())) {
                            recordSecurityEvent(request, "platform.auth.token.rejected",
                                    ActionType.ACCESS_DENIED, AuditActorResolver.unknown(),
                                    AuditEvent.AuditOutcome.DENIED, "AUTH_UNAUTHENTICATED");
                        }
                        response.setContentType("application/json");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"error\":\"Unauthorized\"}");
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        recordSecurityEvent(request, "platform.auth.access.denied",
                                ActionType.ACCESS_DENIED,
                                AuditActorResolver.fromAuthentication(
                                        org.springframework.security.core.context.SecurityContextHolder
                                                .getContext().getAuthentication()),
                                AuditEvent.AuditOutcome.DENIED, "AUTH_FORBIDDEN");
                        response.setContentType("application/json");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("{\"error\":\"Forbidden\"}");
                    }))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**").permitAll();
                    if (apiDocsEnabled) {
                        auth.requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll();
                    }
                    ruleContributors.forEach(contributor -> contributor.contribute(auth));
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void recordSecurityEvent(jakarta.servlet.http.HttpServletRequest request, String action,
            ActionType actionType, AuditEvent.AuditActor actor, AuditEvent.AuditOutcome outcome,
            String errorCode) {
        auditEventRecorder.record(AuditEvent.builder()
                .actor(actor)
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("authz")
                .action(action)
                .actionType(actionType)
                .outcome(outcome)
                .errorCode(errorCode)
                .ipAddress(clientIpResolver.resolveClientIp(request))
                .userAgentClass(AuditClientIpResolver.reduceUserAgent(request.getHeader("User-Agent")))
                .source(AuditEvent.AuditSource.API)
                .build());
    }

    private static boolean isSafeMethod(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
