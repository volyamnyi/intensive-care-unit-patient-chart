package com.superhumans.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.DirContextOperations;

/**
 * Tests for the {@link LdapUserProfileMapper} directory-to-profile mapping
 * contract (decision D5 of issue #244).
 */
@ExtendWith(MockitoExtension.class)
class LdapUserProfileMapperTest {

    @Mock
    private DirContextOperations context;

    @Test
    void fullProfile_mapsAllAttributes() {
        when(context.getStringAttribute("sAMAccountName")).thenReturn("JDoears");
        when(context.getStringAttribute("displayName")).thenReturn("John Doears");
        when(context.getStringAttribute("mail")).thenReturn("john@hospital.ua");
        when(context.getStringAttribute("telephoneNumber")).thenReturn("380501111111");
        when(context.getStringAttribute("title")).thenReturn("Doctor");

        LdapUserProfile profile = LdapUserProfileMapper.mapFromContext(context, "jdoears");

        assertThat(profile.login()).isEqualTo("jdoears");
        assertThat(profile.fullName()).isEqualTo("John Doears");
        assertThat(profile.email()).isEqualTo("john@hospital.ua");
        assertThat(profile.phone()).isEqualTo("380501111111");
        assertThat(profile.specialityName()).isEqualTo("Doctor");
    }

    @Test
    void missingDisplayName_fallsBackToGivenAndFamilyName() {
        when(context.getStringAttribute("sAMAccountName")).thenReturn("jdoe");
        when(context.getStringAttribute("displayName")).thenReturn(null);
        when(context.getStringAttribute("givenName")).thenReturn("John");
        when(context.getStringAttribute("sn")).thenReturn("Doe");

        LdapUserProfile profile = LdapUserProfileMapper.mapFromContext(context, "jdoe");

        assertThat(profile.fullName()).isEqualTo("John Doe");
    }

    @Test
    void missingNames_fallsBackToLogin() {
        when(context.getStringAttribute("sAMAccountName")).thenReturn("JDOE");
        when(context.getStringAttribute("displayName")).thenReturn("  ");
        when(context.getStringAttribute("givenName")).thenReturn(null);
        when(context.getStringAttribute("sn")).thenReturn(null);

        LdapUserProfile profile = LdapUserProfileMapper.mapFromContext(context, "jdoe");

        assertThat(profile.login()).isEqualTo("jdoe");
        assertThat(profile.fullName()).isEqualTo("jdoe");
        assertThat(profile.email()).isNull();
        assertThat(profile.phone()).isNull();
        assertThat(profile.specialityName()).isNull();
    }

    @Test
    void longTitle_isTruncatedToColumnWidth() {
        when(context.getStringAttribute("sAMAccountName")).thenReturn("jdoe");
        when(context.getStringAttribute("displayName")).thenReturn("John Doe");
        when(context.getStringAttribute("title")).thenReturn("T".repeat(300));

        LdapUserProfile profile = LdapUserProfileMapper.mapFromContext(context, "jdoe");

        assertThat(profile.specialityName()).hasSize(LdapUserProfileMapper.SPECIALITY_NAME_MAX_LENGTH);
    }

    @Test
    void missingLoginAttribute_failsClosed() {
        when(context.getStringAttribute("sAMAccountName")).thenReturn(null);

        assertThatThrownBy(() -> LdapUserProfileMapper.mapFromContext(context, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
