package com.superhumans.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AuthManagerConfig {

    private final DaoAuthenticationProvider daoAuthenticationProvider;
    private final LdapAuthenticationProvider ldapAuthenticationProvider;

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(
                List.of(daoAuthenticationProvider, ldapAuthenticationProvider)
        );
    }
}
