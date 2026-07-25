package com.superhumans.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LdapAuthService {

    private final LdapAuthenticationProvider ldapAuthenticationProvider;

    public DirContextOperations authenticate(String username, String password) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(username, password);

        Authentication auth = ldapAuthenticationProvider.authenticate(token);

        return (DirContextOperations) auth.getDetails();
    }
}
