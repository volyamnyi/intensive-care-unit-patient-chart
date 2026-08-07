package com.superhumans.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.access.expression.DefaultHttpSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

@Configuration
public class ClinicalSecurityRules implements SecurityRuleContributor, ApplicationContextAware {

    static final String[] CLINICAL_ROLES = {"DOCTOR", "NURSE", "HEAD_OF_DEPARTMENT", "ADMINISTRATOR", "ADJACENT_SPECIALIST", "PROSTHETIST", "PROSTHETICS_ADMINISTRATOR"};

    /**
     * Roles that read the clinical modules by role (no module permission needed).
     * ADMINISTRATOR/PROSTHETIST/PROSTHETICS_ADMINISTRATOR are deliberately NOT
     * here: for them the admin matrix checkbox (MODULE_*_ACCESS) is the gate.
     */
    static final String CLINICAL_CORE_SPEL =
            "hasAnyRole('DOCTOR','NURSE','HEAD_OF_DEPARTMENT','ADJACENT_SPECIALIST')";

    /** Read access to the ICU chart module: clinical core roles OR the matrix checkbox. */
    static final String ICU_READ_SPEL =
            CLINICAL_CORE_SPEL + " or @permissionService.has('MODULE_ICU_ACCESS')";

    /** Read access to the medication sheet module: clinical core roles OR the matrix checkbox. */
    static final String MEDICATION_READ_SPEL =
            CLINICAL_CORE_SPEL + " or @permissionService.has('MODULE_MEDICATION_ACCESS')";

    /** Patient search is shared by the ICU and medication modules. */
    static final String ICU_OR_MEDICATION_READ_SPEL = CLINICAL_CORE_SPEL
            + " or @permissionService.hasAny('MODULE_ICU_ACCESS','MODULE_MEDICATION_ACCESS')";

    /** User directory reads: clinical core roles or the admin module permission. */
    static final String USERS_READ_SPEL =
            CLINICAL_CORE_SPEL + " or @permissionService.has('MODULE_ADMIN_ACCESS')";

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Builds an {@link AuthorizationManager} from a SpEL expression. Spring
     * Security 7 removed {@code access(String)}, so the expression must be
     * wrapped: the {@link DefaultHttpSecurityExpressionHandler} gets the
     * application context (enabling {@code @permissionService} bean references)
     * and evaluates {@code hasAnyRole(...)} against the current authentication.
     */
    private AuthorizationManager<RequestAuthorizationContext> spEl(String expression) {
        DefaultHttpSecurityExpressionHandler handler = new DefaultHttpSecurityExpressionHandler();
        handler.setApplicationContext(this.applicationContext);
        WebExpressionAuthorizationManager manager = new WebExpressionAuthorizationManager(expression);
        manager.setExpressionHandler(handler);
        return manager;
    }

    @Override
    public void contribute(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                // ---- Module-visit read rules (dynamic RBAC) ----
                // The admin matrix checkbox (MODULE_*_ACCESS) grants a role the
                // ability to VISIT the module: the frontend gates the routes by
                // the same permissions, and the module's read paths accept them
                // here. Clinical core roles keep their read access by role;
                // write endpoints remain gated by the ceiling + @PreAuthorize.
                .requestMatchers(HttpMethod.GET, "/api/episodes/**").access(spEl(ICU_READ_SPEL))
                .requestMatchers(HttpMethod.GET, "/api/clinical-days/**").access(spEl(ICU_READ_SPEL))
                .requestMatchers(HttpMethod.GET, "/api/notes/**").access(spEl(ICU_READ_SPEL))
                .requestMatchers(HttpMethod.GET, "/api/scales/**").access(spEl(ICU_READ_SPEL))
                .requestMatchers(HttpMethod.GET, "/api/hourly-records/**").access(spEl(ICU_READ_SPEL))
                .requestMatchers(HttpMethod.GET, "/api/orders/**").access(spEl(ICU_READ_SPEL))
                .requestMatchers(HttpMethod.GET, "/api/patients/**").access(spEl(ICU_OR_MEDICATION_READ_SPEL))
                // /api/users/me + /api/users/me/permissions must load for every
                // authenticated user (AuthContext), regardless of role or matrix.
                .requestMatchers(HttpMethod.GET, "/api/users/me/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/users/**").access(spEl(USERS_READ_SPEL))
                .requestMatchers(HttpMethod.GET, "/api/prescriptions/**").access(spEl(MEDICATION_READ_SPEL))
                .requestMatchers(HttpMethod.GET, "/api/vital-signs/**").access(spEl(ICU_READ_SPEL))
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