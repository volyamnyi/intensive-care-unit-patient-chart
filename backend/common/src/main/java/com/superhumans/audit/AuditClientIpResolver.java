package com.superhumans.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves the client IP literal for security audit events.
 *
 * <p>Forwarded headers are trusted only when the socket peer belongs to the configured
 * allowlist of proxy CIDRs/IPs ({@code app.audit.trusted-proxy-cidrs}). Without an
 * allowlist the socket peer is used as-is; client-supplied {@code X-Forwarded-For}
 * is never trusted from an unknown peer (decisions D3/D7).
 */
@Slf4j
@Component
public class AuditClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9A-Fa-f:]{2,45}$");

    private final List<Subnet> trustedProxies;

    public AuditClientIpResolver(
            @Value("${app.audit.trusted-proxy-cidrs:}") String trustedProxyCidrs) {
        this.trustedProxies = parseAllowlist(trustedProxyCidrs);
    }

    public String resolveClientIp(HttpServletRequest request) {
        String peer = trimToNull(request.getRemoteAddr());
        if (!isTrustedProxy(peer)) {
            return isIpLiteral(peer) ? peer : null;
        }
        // #361: walk the chain from the RIGHT. Each proxy APPENDS the address it
        // received from, so the rightmost entries come from the proxies we trust
        // and the leftmost entries are client-supplied. Taking the first literal
        // (the old behaviour) let a client put any IP ahead of the real one and
        // have the audit store it. The rightmost entry that is NOT itself a
        // trusted proxy is the best candidate for the true client; if the whole
        // chain is trusted proxies, fall back to the leftmost literal.
        String forwarded = trimToNull(request.getHeader("X-Forwarded-For"));
        if (forwarded != null) {
            String[] entries = forwarded.split(",");
            String leftmostLiteral = null;
            for (int i = entries.length - 1; i >= 0; i--) {
                String ip = trimToNull(entries[i]);
                if (!isIpLiteral(ip)) {
                    continue;
                }
                if (!isTrustedProxy(ip)) {
                    return ip;
                }
                // Walking right-to-left, the last assignment wins - so this
                // ends up holding the LEFTMOST literal of the chain.
                leftmostLiteral = ip;
            }
            if (leftmostLiteral != null) {
                return leftmostLiteral;
            }
        }
        return isIpLiteral(peer) ? peer : null;
    }

    /**
     * Reduces a raw User-Agent header to a bounded device class.
     * Raw headers are never stored in audit events (decision D3).
     */
    public static String reduceUserAgent(String rawUserAgent) {
        if (rawUserAgent == null || rawUserAgent.isBlank() || rawUserAgent.length() > 512) {
            return null;
        }
        String lower = rawUserAgent.toLowerCase(Locale.ROOT);
        if (lower.contains("bot") || lower.contains("crawl") || lower.contains("spider")
                || lower.contains("slurp") || lower.contains("mediapartners")) {
            return "bot";
        }
        if (lower.contains("mobile") || lower.contains("android")
                || lower.contains("iphone") || lower.contains("ipad")) {
            return "mobile";
        }
        return "desktop";
    }

    private boolean isTrustedProxy(String peer) {
        if (peer == null || trustedProxies.isEmpty()) {
            return false;
        }
        for (Subnet subnet : trustedProxies) {
            try {
                if (subnet.contains(InetAddress.getByName(peer))) {
                    return true;
                }
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    static boolean isIpLiteral(String value) {
        if (value == null || value.length() > 45) {
            return false;
        }
        if (IPV4.matcher(value).matches()) {
            for (String octet : value.split("\\.")) {
                int parsed = Integer.parseInt(octet);
                if (parsed < 0 || parsed > 255) {
                    return false;
                }
            }
            return true;
        }
        return IPV6.matcher(value).matches() && value.indexOf(':') >= 0;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<Subnet> parseAllowlist(String raw) {
        List<Subnet> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String entry : raw.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Subnet.parse(trimmed));
            } catch (IllegalArgumentException exception) {
                log.warn("Ignoring invalid trusted proxy CIDR entry");
            }
        }
        return result;
    }

    record Subnet(InetAddress network, int prefixLength) {
        static Subnet parse(String value) {
            try {
                if (value.contains("/")) {
                    String[] parts = value.split("/", 2);
                    InetAddress network = InetAddress.getByName(parts[0].trim());
                    int prefix = Integer.parseInt(parts[1].trim());
                    int bits = network.getAddress().length * 8;
                    if (prefix < 0 || prefix > bits) {
                        throw new IllegalArgumentException("Bad prefix: " + value);
                    }
                    return new Subnet(network, prefix);
                }
                return new Subnet(InetAddress.getByName(value), -1);
            } catch (IllegalArgumentException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalArgumentException("Bad trusted proxy entry: " + value, exception);
            }
        }

        boolean contains(InetAddress address) {
            byte[] networkBytes = network.getAddress();
            byte[] addressBytes = address.getAddress();
            if (networkBytes.length != addressBytes.length) {
                return false;
            }
            if (prefixLength < 0) {
                for (int i = 0; i < networkBytes.length; i++) {
                    if (networkBytes[i] != addressBytes[i]) {
                        return false;
                    }
                }
                return true;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (networkBytes[i] != addressBytes[i]) {
                    return false;
                }
            }
            if (remainingBits > 0) {
                int mask = 0xFF << (8 - remainingBits);
                return (networkBytes[fullBytes] & mask) == (addressBytes[fullBytes] & mask);
            }
            return true;
        }
    }
}
