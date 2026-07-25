package com.superhumans.utils;

import com.superhumans.model.user.LdapUserDetailsAdapter;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.ArrayList;
import java.util.Collection;

public class ADUserContextMapper implements UserDetailsContextMapper {

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                          Collection<? extends GrantedAuthority> authorities) {

        String firstName = ctx.getStringAttribute("givenName");
        String lastName  = ctx.getStringAttribute("sn");
        String email     = ctx.getStringAttribute("mail");

        return new LdapUserDetailsAdapter(
                username,
                firstName,
                lastName,
                email,
                authorities
        );
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {

    }

}
