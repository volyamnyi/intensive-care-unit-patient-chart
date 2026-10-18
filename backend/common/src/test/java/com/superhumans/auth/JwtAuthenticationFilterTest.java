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
        assertThat(captor.getValue().errorCode()).isEqualTo("AUTH_TOKEN_REJECTED");
    }

    @Test
    void coreDbOutage_degradesToUnauthenticatedInsteadOfA500() throws Exception {
        // #367: the live-user re-check threw DataAccessException before; the
        // exception left the filter and every authenticated request died as a
        // container 500. The chain must complete, unauthenticated - one uniform
        // fail-closed 401 - and the outage must leave an audit trace.
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 86400000);
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(11L)).thenThrow(
                new org.springframework.dao.DataAccessResourceFailureException("core db down"));
        ObjectProvider<UserRepository> userRepositoryProvider = mock(ObjectProvider.class);
        when(userRepositoryProvider.getIfAvailable()).thenReturn(userRepository);
        ObjectProvider<TokenRevocationService> revocationProvider = mock(ObjectProvider.class);
        when(revocationProvider.getIfAvailable()).thenReturn(new TokenRevocationService());
        com.superhumans.audit.AuditEventRecorder recorder =
                mock(com.superhumans.audit.AuditEventRecorder.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                provider, mockRecorderProvider(recorder),
                mockResolverProvider(mock(com.superhumans.audit.AuditClientIpResolver.class)),
                userRepositoryProvider, revocationProvider);

        String token = provider.generateToken("doctor1", "DOCTOR", 11L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/episodes");
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        // Must not throw - that was the whole bug.
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        org.mockito.Mockito.verify(chain).doFilter(org.mockito.ArgumentMatchers.eq(request), org.mockito.ArgumentMatchers.any());

        var captor = org.mockito.ArgumentCaptor
                .forClass(com.superhumans.audit.AuditEvent.class);
        org.mockito.Mockito.verify(recorder).record(captor.capture());
        assertThat(captor.getValue().errorCode()).isEqualTo("AUTH_USER_STORE_UNAVAILABLE");
        assertThat(captor.getValue().outcome())
                .isEqualTo(com.superhumans.audit.AuditEvent.AuditOutcome.DENIED);
    }

    @Test
    void coreDbOutage_auditAlsoDown_stillDegradesWithoutPropagating() throws Exception {
        // The audit store shares the dead database; the filter must swallow its
        // failure too - the uniform 401 matters more than the audit row.
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 86400000);
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(11L)).thenThrow(
                new org.springframework.dao.DataAccessResourceFailureException("core db down"));
        ObjectProvider<UserRepository> userRepositoryProvider = mock(ObjectProvider.class);
        when(userRepositoryProvider.getIfAvailable()).thenReturn(userRepository);
        ObjectProvider<TokenRevocationService> revocationProvider = mock(ObjectProvider.class);
        when(revocationProvider.getIfAvailable()).thenReturn(new TokenRevocationService());
        com.superhumans.audit.AuditEventRecorder recorder =
                mock(com.superhumans.audit.AuditEventRecorder.class);
        org.mockito.Mockito.doThrow(new RuntimeException("audit store down"))
                .when(recorder).record(org.mockito.ArgumentMatchers.any());
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                provider, mockRecorderProvider(recorder),
                mockResolverProvider(mock(com.superhumans.audit.AuditClientIpResolver.class)),
                userRepositoryProvider, revocationProvider);

        String token = provider.generateToken("doctor1", "DOCTOR", 11L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/episodes");
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        org.mockito.Mockito.verify(chain).doFilter(org.mockito.ArgumentMatchers.eq(request), org.mockito.ArgumentMatchers.any());
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
