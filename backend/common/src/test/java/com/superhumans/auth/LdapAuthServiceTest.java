package com.superhumans.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

/**
 * Tests for the {@link LdapAuthService} closed-result authentication contract:
 * success yields a profile, any failure yields empty without leaking details.
 *
 * <p>Local-only suite: the corporate directory is unreachable from CI
 * runners, so every LDAP test runs exclusively locally and is skipped
 * otherwise. Run with
 * {@code mvn test -pl common -Dldap.local.tests=true -Dtest='Ldap*Test'}
 * from the {@code backend} directory.
 */
@EnabledIfSystemProperty(named = "ldap.local.tests", matches = "true")
@ExtendWith(MockitoExtension.class)
class LdapAuthServiceTest {

    @Mock
    private LdapAuthenticationProvider provider;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LdapAuthService service;

    @Test
    void successfulBind_returnsProfile() {
        LdapUserProfile profile = new LdapUserProfile("jdoe", "John Doe", null, null, null);
        when(provider.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new LdapUserDetails(profile));

        Optional<LdapUserProfile> result = service.authenticate("jdoe", "secret");

        assertThat(result).contains(profile);
    }

    @Test
    void invalidCredentials_returnsEmpty() {
        when(provider.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        Optional<LdapUserProfile> result = service.authenticate("jdoe", "wrong");

        assertThat(result).isEmpty();
    }

    @Test
    void directoryOutage_returnsEmptyWithoutPropagating() {
        when(provider.authenticate(any()))
                .thenThrow(new AuthenticationServiceException("ldap://directory.example:389 unreachable"));

        Optional<LdapUserProfile> result = service.authenticate("jdoe", "secret");

        assertThat(result).isEmpty();
    }

    @Test
    void blankCredentials_shortCircuitWithoutProviderCall() {
        assertThat(service.authenticate("  ", "secret")).isEmpty();
        assertThat(service.authenticate("jdoe", null)).isEmpty();
        verifyNoInteractions(provider);
    }
}
