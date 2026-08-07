package com.superhumans.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@Configuration
public class ClinicalSecurityRules implements SecurityRuleContributor {

    static final String[] CLINICAL_ROLES = {"DOCTOR", "NURSE", "HEAD_OF_DEPARTMENT", "ADMINISTRATOR", "ADJACENT_SPECIALIST", "PROSTHETIST", "PROSTHETICS_ADMINISTRATOR"};

    @Override
    public void contribute(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // Episode management. URL rules act as a ceiling only; the precise
                // matrix (which roles may actually create) is enforced dynamically
                // by @PreAuthorize("@permissionService.has('...')") so that admin
                // changes to role permissions take effect immediately.
                .requestMatchers(HttpMethod.POST, "/api/episodes").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers("/api/episodes/**").hasAnyRole(CLINICAL_ROLES)
                // Clinical day management
                .requestMatchers(HttpMethod.POST, "/api/clinical-days").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.POST, "/api/clinical-days/*/sign/**").hasAnyRole(CLINICAL_ROLES)
                // Order creation via clinical-days
                .requestMatchers(HttpMethod.POST, "/api/clinical-days/*/orders").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers("/api/clinical-days/**").hasAnyRole(CLINICAL_ROLES)
                // Notes (PATCH)
                .requestMatchers("/api/notes/**").hasAnyRole(CLINICAL_ROLES)
                // Scales
                .requestMatchers("/api/scales/**").hasAnyRole(CLINICAL_ROLES)
                // Hourly records (PATCH)
                .requestMatchers("/api/hourly-records/**").hasAnyRole(CLINICAL_ROLES)
                // Medical orders
                .requestMatchers(HttpMethod.POST, "/api/orders/*/execute/finish").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.POST, "/api/orders/*/execute").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/plan/finish").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/plan").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/cancel").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers("/api/orders/**").hasAnyRole(CLINICAL_ROLES)
                // Order executions (PATCH)
                .requestMatchers(HttpMethod.PATCH, "/api/executions/**").hasAnyRole(CLINICAL_ROLES)
                // Audit - admins (matrix: AUDIT_ACCESS) and auditors
                .requestMatchers("/api/audit/**").hasAnyRole("ADMINISTRATOR", "AUDITOR")
                // Patient search from MIS
                .requestMatchers("/api/patients/**").hasAnyRole(CLINICAL_ROLES)
                // Users
                .requestMatchers("/api/users/**").hasAnyRole(CLINICAL_ROLES)
                // Prescription module
                .requestMatchers(HttpMethod.POST, "/api/prescriptions").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.POST, "/api/prescriptions/*/items").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.PUT, "/api/prescriptions/day-parts/*/plan").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.PUT, "/api/prescriptions/day-parts/*/complete").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.POST, "/api/prescriptions/day-parts/*/execute").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.POST, "/api/prescriptions/*/close").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.DELETE, "/api/prescriptions/items/*").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers(HttpMethod.DELETE, "/api/prescriptions/*").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers("/api/prescriptions/**").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers("/api/vital-signs/**").hasAnyRole(CLINICAL_ROLES)
                .requestMatchers("/api/admin/**").hasAnyRole("ADMINISTRATOR")
                .requestMatchers("/api/**").hasAnyRole(CLINICAL_ROLES);
    }
}