package com.superhumans.auth;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * {@link UserDetails} carrier for a successfully bound directory user.
 *
 * <p>Exists only because {@code LdapAuthenticationProvider} requires a
 * {@code UserDetailsContextMapper} result. It wraps the read-only
 * {@link LdapUserProfile} and deliberately exposes no password
 * (empty string) and no authorities (empty list): directory groups are
 * never used as application authorization (decision D3 of issue #244).
 * The instance is transient — it is never persisted.
 */
public final class LdapUserDetails implements UserDetails {

    private final LdapUserProfile profile;

    /**
     * Creates carrier details for the given directory profile.
     *
     * @param profile read-only directory profile snapshot, must not be {@code null}
     */
    public LdapUserDetails(LdapUserProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("LDAP profile must not be null");
        }
        this.profile = profile;
    }

    /**
     * Returns the wrapped read-only directory profile snapshot.
     *
     * @return directory profile, never {@code null}
     */
    public LdapUserProfile getProfile() {
        return profile;
    }

    @Override
    public String getUsername() {
        return profile.login();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
