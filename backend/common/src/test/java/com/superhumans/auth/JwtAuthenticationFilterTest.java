package com.superhumans.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import jakarta.servlet.FilterChain;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "cGF0aWVudC1jaGFydC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi0yMDI2";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void demotedUserIsNotAuthenticatedWithOldToken() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 86400000);
        UserRepository userRepository = mock(UserRepository.class);
        User user = User.builder().role(UserRole.NURSE).deleted(false).build();
        user.setId(11L);
        when(userRepository.findById(11L)).thenReturn(java.util.Optional.of(user));

        ObjectProvider<UserRepository> userRepositoryProvider = mock(ObjectProvider.class);
        when(userRepositoryProvider.getIfAvailable()).thenReturn(userRepository);
        ObjectProvider<TokenRevocationService> revocationProvider = mock(ObjectProvider.class);
        when(revocationProvider.getIfAvailable()).thenReturn(new TokenRevocationService());
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                provider, mockRecorderProvider(null), mockResolverProvider(null),
                userRepositoryProvider, revocationProvider);
        String token = provider.generateToken("doctor1", "DOCTOR", 11L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void revokedTokenOnMutation_recordsTokenRejectedWithoutTokenValue() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 86400000);
        ObjectProvider<UserRepository> userRepositoryProvider = mock(ObjectProvider.class);
        when(userRepositoryProvider.getIfAvailable()).thenReturn(mock(UserRepository.class));
        TokenRevocationService revocationService = new TokenRevocationService();
        ObjectProvider<TokenRevocationService> revocationProvider = mock(ObjectProvider.class);
        when(revocationProvider.getIfAvailable()).thenReturn(revocationService);
        com.superhumans.audit.AuditEventRecorder recorder =
                mock(com.superhumans.audit.AuditEventRecorder.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                provider, mockRecorderProvider(recorder),
                mockResolverProvider(mock(com.superhumans.audit.AuditClientIpResolver.class)),
                userRepositoryProvider, revocationProvider);
        String token = provider.generateToken("doctor1", "DOCTOR", 11L);
        revocationService.revoke(provider.getJtiFromToken(token),
                provider.getExpirationFromToken(token).toInstant());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/episodes");
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        var captor = org.mockito.ArgumentCaptor
                .forClass(com.superhumans.audit.AuditEvent.class);
        org.mockito.Mockito.verify(recorder).record(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("platform.auth.token.rejected");
        assertThat(captor.getValue().outcome())
                .isEqualTo(com.superhumans.audit.AuditEvent.AuditOutcome.DENIED);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<com.superhumans.audit.AuditEventRecorder> mockRecorderProvider(
            com.superhumans.audit.AuditEventRecorder recorder) {
        ObjectProvider<com.superhumans.audit.AuditEventRecorder> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(recorder);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<com.superhumans.audit.AuditClientIpResolver> mockResolverProvider(
            com.superhumans.audit.AuditClientIpResolver resolver) {
        ObjectProvider<com.superhumans.audit.AuditClientIpResolver> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(resolver);
        return provider;
    }
}
