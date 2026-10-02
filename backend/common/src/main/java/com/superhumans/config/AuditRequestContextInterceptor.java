package com.superhumans.config;

import com.superhumans.audit.AuditRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/** Captures Spring's route template before a controller invokes domain services. */
@Component
public class AuditRequestContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AuditRequestContext.Context context = AuditRequestContext.current();
        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (context != null && route != null) {
            context.setRouteTemplate(route.toString());
        }
        return true;
    }
}
