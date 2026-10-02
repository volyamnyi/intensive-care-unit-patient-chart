package com.superhumans.config;

import com.superhumans.audit.AuditClientIpResolver;
import com.superhumans.audit.AuditRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** Adds per-request IDs and emits body-free technical access completion logs. */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String USER_ACTION_ID_HEADER = "X-User-Action-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final ObjectProvider<AuditClientIpResolver> clientIpResolverProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        UUID requestId = UUID.randomUUID();
        UUID userActionId = parseUuid(request.getHeader(USER_ACTION_ID_HEADER));
        UUID correlationId = userActionId == null ? requestId : userActionId;
        long startedAt = System.nanoTime();
        var context = new AuditRequestContext.Context(
                requestId, userActionId, correlationId, startedAt, request.getMethod(),
                resolveClientIp(request));
        response.setHeader(REQUEST_ID_HEADER, requestId.toString());
        response.setHeader(CORRELATION_ID_HEADER, correlationId.toString());
        request.setAttribute(AuditRequestContext.REQUEST_ATTRIBUTE, context);

        try (AuditRequestContext.Scope ignored = AuditRequestContext.install(context)) {
            MDC.put("requestId", requestId.toString());
            MDC.put("correlationId", correlationId.toString());
            if (userActionId != null) {
                MDC.put("userActionId", userActionId.toString());
            }
            boolean failed = false;
            try {
                filterChain.doFilter(request, response);
            } catch (IOException | ServletException | RuntimeException exception) {
                failed = true;
                throw exception;
            } finally {
                long durationMs = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
                String route = routeTemplate(request);
                log.info("http_request_completed requestId={} correlationId={} userActionId={} "
                                + "method={} route={} status={} failed={} durationMs={}",
                        requestId, correlationId, userActionId, request.getMethod(), route,
                        response.getStatus(), failed, durationMs);
                MDC.remove("userActionId");
                MDC.remove("correlationId");
                MDC.remove("requestId");
            }
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        AuditClientIpResolver resolver = clientIpResolverProvider.getIfAvailable();
        return resolver == null ? null : resolver.resolveClientIp(request);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank() || value.length() > 36) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String routeTemplate(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern instanceof String route ? route : "/<unmatched>";
    }
}
