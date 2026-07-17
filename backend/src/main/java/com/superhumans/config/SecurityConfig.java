package com.superhumans.config;

import com.superhumans.auth.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    JwtAuthenticationFilter jwtAuthFilter;

    private static final String[] CLINICAL_ROLES = {"DOCTOR", "NURSE", "HEAD_OF_DEPARTMENT", "ADMINISTRATOR"};
    private static final String[] PRESCRIBER_ROLES = {"DOCTOR", "HEAD_OF_DEPARTMENT"};
    private static final String[] SIGNER_ROLES = {"DOCTOR", "HEAD_OF_DEPARTMENT"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setContentType("application/json");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"error\":\"Unauthorized\"}");
                    }))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // Episode management
                        .requestMatchers(HttpMethod.POST, "/api/episodes").hasAnyRole(PRESCRIBER_ROLES)
                        .requestMatchers("/api/episodes/**").hasAnyRole(CLINICAL_ROLES)
                        // Clinical day management
                        .requestMatchers(HttpMethod.POST, "/api/clinical-days").hasAnyRole(CLINICAL_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/clinical-days/*/sign/**").hasAnyRole(CLINICAL_ROLES)
                        // Order creation via clinical-days requires prescriber
                        .requestMatchers(HttpMethod.POST, "/api/clinical-days/*/orders").hasAnyRole(PRESCRIBER_ROLES)
                        .requestMatchers("/api/clinical-days/**").hasAnyRole(CLINICAL_ROLES)
                        // Notes (PATCH)
                        .requestMatchers("/api/notes/**").hasAnyRole(CLINICAL_ROLES)
                        // Scales
                        .requestMatchers("/api/scales/**").hasAnyRole(CLINICAL_ROLES)
                        // Hourly records (PATCH)
                        .requestMatchers("/api/hourly-records/**").hasAnyRole(CLINICAL_ROLES)
                        // Medical orders
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/execute").hasAnyRole(CLINICAL_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole(PRESCRIBER_ROLES)
                        .requestMatchers("/api/orders/**").hasAnyRole(CLINICAL_ROLES)
                        // Order executions (PATCH)
                        .requestMatchers("/api/executions/**").hasAnyRole(CLINICAL_ROLES)
                        // Audit - admins and auditors
                        .requestMatchers("/api/audit/**").hasAnyRole("ADMINISTRATOR", "AUDITOR")
                        // Patient search from MIS
                        .requestMatchers("/api/patients/**").hasAnyRole(CLINICAL_ROLES)
                        // Users
                        .requestMatchers("/api/users/**").hasAnyRole(CLINICAL_ROLES)
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
