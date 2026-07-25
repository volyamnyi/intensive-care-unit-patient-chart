package com.superhumans.config;

import com.superhumans.utils.ADUserContextMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.PersonContextMapper;

@Configuration
public class LdapConfig {

    @Value("${spring.ldap.urls}")
    private String ldapUrl;

    @Value("${spring.ldap.base}")
    private String ldapBase;

    @Value("${spring.ldap.username}")
    private String ldapBindDn;

    @Value("${spring.ldap.password}")
    private String ldapBindPassword;

    @Bean
    public LdapContextSource contextSource() {
        LdapContextSource cs = new LdapContextSource();
        cs.setUrl(ldapUrl);
        cs.setBase(ldapBase);
        cs.setUserDn(ldapBindDn);
        cs.setPassword(ldapBindPassword);
        cs.afterPropertiesSet();
        return cs;
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider() {
        BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource());
        bindAuthenticator.setUserSearch(new FilterBasedLdapUserSearch(
                "",
                "(sAMAccountName={0})",
                contextSource()
        ));

        DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource(), null);
        authoritiesPopulator.setIgnorePartialResultException(true);

        LdapAuthenticationProvider provider =
                new LdapAuthenticationProvider(bindAuthenticator, authoritiesPopulator);

        provider.setUserDetailsContextMapper(new ADUserContextMapper());

        return provider;
    }

}
