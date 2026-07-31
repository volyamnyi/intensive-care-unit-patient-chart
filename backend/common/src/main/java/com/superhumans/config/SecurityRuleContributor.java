package com.superhumans.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Contributor hook that lets dependent modules register their own URL-based authorization
 * rules on top of the generic {@link SecurityConfig} filter chain.
 *
 * <p>Implementations are discovered as Spring beans and applied in order, before the
 * fallback {@code anyRequest().authenticated()} rule.
 */
@FunctionalInterface
public interface SecurityRuleContributor {

    void contribute(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>
                    .AuthorizationManagerRequestMatcherRegistry registry);
}
