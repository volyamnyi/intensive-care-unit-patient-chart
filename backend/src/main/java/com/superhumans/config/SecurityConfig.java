package com.superhumans.config;

import com.superhumans.auth.JwtAuthenticationFilter;
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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    private static final String[] CLINICAL_ROLES = {"DOCTOR", "NURSE", "HEAD_OF_DEPARTMENT", "ADMINISTRATOR"};
    private static final String[] PRESCRIBER_ROLES = {"DOCTOR", "HEAD_OF_DEPARTMENT"};
    private static final String[] SIGNER_ROLES = {"DOCTOR", "HEAD_OF_DEPARTMENT"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // ICU card creation is limited to doctors / head of department
                        .requestMatchers(HttpMethod.POST, "/api/icu-cards").hasAnyRole(PRESCRIBER_ROLES)
                        .requestMatchers("/api/icu-cards/**").hasAnyRole(CLINICAL_ROLES)
                        // Patient search is visible to every clinical role (incl. administrator)
                        .requestMatchers("/api/patients/search").hasAnyRole(CLINICAL_ROLES)
                        // Prescription execution is performed by nurses as well
                        .requestMatchers(HttpMethod.POST, "/api/prescriptions/*/execute").hasAnyRole("DOCTOR", "NURSE", "HEAD_OF_DEPARTMENT")
                        // Prescription creation / stop is limited to doctors / head of department
                        .requestMatchers(HttpMethod.POST, "/api/prescriptions/**").hasAnyRole(PRESCRIBER_ROLES)
                        .requestMatchers("/api/prescriptions/**").hasAnyRole(CLINICAL_ROLES)
                        // Day sign-off is limited to doctors / head of department.
                        // This matcher MUST precede the general "/api/icu-days/**" rule, otherwise
                        // the more specific restriction would be shadowed by the wildcard.
                        .requestMatchers(HttpMethod.POST, "/api/icu-days/*/sign-off").hasAnyRole(SIGNER_ROLES)
                        .requestMatchers("/api/icu-days/**").hasAnyRole(CLINICAL_ROLES)
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
