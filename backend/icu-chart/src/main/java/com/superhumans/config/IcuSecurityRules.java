package com.superhumans.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@Configuration
public class IcuSecurityRules implements SecurityRuleContributor {

    static final String[] CLINICAL_ROLES = {"DOCTOR", "NURSE", "HEAD_OF_DEPARTMENT", "ADMINISTRATOR", "ADJACENT_SPECIALIST"};
    static final String[] PRESCRIBER_ROLES = {"DOCTOR", "HEAD_OF_DEPARTMENT"};
    static final String[] SIGNER_ROLES = {"DOCTOR", "HEAD_OF_DEPARTMENT"};
    static final String[] EXECUTOR_ROLES = {"NURSE", "HEAD_OF_DEPARTMENT"};

    @Override
    public void contribute(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry registry) {
        registry
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
                // Prescription module
                .requestMatchers(HttpMethod.POST, "/api/prescriptions").hasAnyRole(PRESCRIBER_ROLES)
                .requestMatchers(HttpMethod.POST, "/api/prescriptions/*/items").hasAnyRole(PRESCRIBER_ROLES)
                .requestMatchers(HttpMethod.PUT, "/api/prescriptions/day-parts/*/plan").hasAnyRole(PRESCRIBER_ROLES)
                .requestMatchers(HttpMethod.PUT, "/api/prescriptions/day-parts/*/complete").hasAnyRole(EXECUTOR_ROLES)
                .requestMatchers(HttpMethod.POST, "/api/prescriptions/day-parts/*/execute").hasAnyRole(EXECUTOR_ROLES)
                .requestMatchers(HttpMethod.POST, "/api/prescriptions/*/close").hasAnyRole(PRESCRIBER_ROLES)
                .requestMatchers(HttpMethod.DELETE, "/api/prescriptions/items/*").hasAnyRole(PRESCRIBER_ROLES)
                .requestMatchers(HttpMethod.DELETE, "/api/prescriptions/*").hasAnyRole(PRESCRIBER_ROLES)
                .requestMatchers("/api/prescriptions/**").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers("/api/vital-signs/**").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers("/api/admin/**").hasAnyRole("ADMINISTRATOR")
                .requestMatchers("/api/**").hasAnyRole(CLINICAL_ROLES);
    }
}
