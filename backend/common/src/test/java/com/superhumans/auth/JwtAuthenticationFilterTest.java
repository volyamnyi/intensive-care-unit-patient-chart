package com.superhumans.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.service.AuditService;
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
                provider, mock(AuditLogRepository.class), mock(AuditService.class),
                userRepositoryProvider, revocationProvider);
        String token = provider.generateToken("doctor1", "DOCTOR", 11L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
