package com.superhumans.controller;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

public final class TestSecurityHelper {

    private static final String TEST_JWT_DOCTOR = "test-jwt-token";
    private static final String TEST_JWT_NURSE = "test-nurse-token";
    private static final String TEST_JWT_ADMIN = "test-admin-token";
    private static final String TEST_JWT_HOD = "test-hod-token";

    private TestSecurityHelper() {}

    public static RequestPostProcessor doctor() {
        return authenticatedUser("user", 1L, "ROLE_DOCTOR", TEST_JWT_DOCTOR);
    }

    public static RequestPostProcessor nurse() {
        return authenticatedUser("user", 2L, "ROLE_NURSE", TEST_JWT_NURSE);
    }

    public static RequestPostProcessor admin() {
        return authenticatedUser("user", 3L, "ROLE_ADMINISTRATOR", TEST_JWT_ADMIN);
    }

    public static RequestPostProcessor hod() {
        return authenticatedUser("user", 4L, "ROLE_HEAD_OF_DEPARTMENT", TEST_JWT_HOD);
    }

    public static RequestPostProcessor auditor() {
        return authenticatedUser("user", 5L, "ROLE_AUDITOR", "test-auditor-token");
    }

    private static RequestPostProcessor authenticatedUser(String login, Long userId, String role, String jwt) {
        return request -> {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    login, userId, List.of(new SimpleGrantedAuthority(role)));
            MockHttpServletRequest req = (MockHttpServletRequest) authentication(token).postProcessRequest(request);
            req.addHeader("Authorization", "Bearer " + jwt);
            SecurityContextHolder.getContext().setAuthentication(token);
            return req;
        };
    }
}