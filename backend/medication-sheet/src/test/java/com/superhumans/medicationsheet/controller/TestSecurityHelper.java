package com.superhumans.medicationsheet.controller;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

public final class TestSecurityHelper {

    private static final String TEST_JWT_DOCTOR = "test-jwt-token";
    private static final String TEST_JWT_NURSE = "test-nurse-token";
    private static final String TEST_JWT_ADMIN = "test-admin-token";

    private TestSecurityHelper() {}

    public static RequestPostProcessor doctor() {
        return request -> {
            MockHttpServletRequest req = (MockHttpServletRequest)
                    authentication(new UsernamePasswordAuthenticationToken(
                            "user", 1L, List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                            .postProcessRequest(request);
            req.addHeader("Authorization", "Bearer " + TEST_JWT_DOCTOR);
            return req;
        };
    }

    public static RequestPostProcessor nurse() {
        return request -> {
            MockHttpServletRequest req = (MockHttpServletRequest)
                    authentication(new UsernamePasswordAuthenticationToken(
                            "user", 2L, List.of(new SimpleGrantedAuthority("ROLE_NURSE"))))
                            .postProcessRequest(request);
            req.addHeader("Authorization", "Bearer " + TEST_JWT_NURSE);
            return req;
        };
    }

    public static RequestPostProcessor admin() {
        return request -> {
            MockHttpServletRequest req = (MockHttpServletRequest)
                    authentication(new UsernamePasswordAuthenticationToken(
                            "user", 3L, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                            .postProcessRequest(request);
            req.addHeader("Authorization", "Bearer " + TEST_JWT_ADMIN);
            return req;
        };
    }
}
