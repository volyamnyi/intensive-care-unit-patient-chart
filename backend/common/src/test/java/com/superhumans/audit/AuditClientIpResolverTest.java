package com.superhumans.audit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuditClientIpResolverTest {

    @Test
    void untrustedPeerUsesSocketAddressAndIgnoresForwardedHeader() {
        var resolver = new AuditClientIpResolver("10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.5");

        assertThat(resolver.resolveClientIp(request)).isEqualTo("198.51.100.7");
    }

    @Test
    void trustedProxyUsesLeftmostValidForwardedAddress() {
        var resolver = new AuditClientIpResolver("10.0.0.0/8, 2001:db8::1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "unknown, 203.0.113.9, 10.0.0.6");

        assertThat(resolver.resolveClientIp(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void emptyAllowlistNeverTrustsForwardedHeaders() {
        var resolver = new AuditClientIpResolver("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        assertThat(resolver.resolveClientIp(request)).isEqualTo("10.0.0.5");
    }

    @Test
    void invalidPeerAndHeaderYieldNull() {
        var resolver = new AuditClientIpResolver("10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("not-an-ip");
        request.addHeader("X-Forwarded-For", "also-not-an-ip");

        assertThat(resolver.resolveClientIp(request)).isNull();
    }

    @Test
    void userAgentIsReducedToDeviceClass() {
        assertThat(AuditClientIpResolver.reduceUserAgent(null)).isNull();
        assertThat(AuditClientIpResolver.reduceUserAgent(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)")).isEqualTo("mobile");
        assertThat(AuditClientIpResolver.reduceUserAgent("Googlebot/2.1")).isEqualTo("bot");
        assertThat(AuditClientIpResolver.reduceUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")).isEqualTo("desktop");
    }
}
