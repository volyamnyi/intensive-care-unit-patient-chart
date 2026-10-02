package com.superhumans.config;

import com.superhumans.audit.AuditRequestContext;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter =
            new RequestCorrelationFilter(mockProvider(new com.superhumans.audit.AuditClientIpResolver("")));

    @SuppressWarnings("unchecked")
    private static org.springframework.beans.factory.ObjectProvider<
            com.superhumans.audit.AuditClientIpResolver> mockProvider(
            com.superhumans.audit.AuditClientIpResolver resolver) {
        org.springframework.beans.factory.ObjectProvider<
                com.superhumans.audit.AuditClientIpResolver> provider =
                org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(resolver);
        return provider;
    }

    @Test
    void assignsRequestIdAndPropagatesValidUserActionIdWithoutTrustingClientRequestId() throws Exception {
        UUID actionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/episodes/123/close");
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "22222222-2222-2222-2222-222222222222");
        request.addHeader(RequestCorrelationFilter.USER_ACTION_ID_HEADER, actionId.toString());
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/episodes/{id}/close");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            new AuditRequestContextInterceptor().preHandle(
                    (jakarta.servlet.http.HttpServletRequest) req,
                    (jakarta.servlet.http.HttpServletResponse) res,
                    new Object());
            AuditRequestContext.Context context = AuditRequestContext.current();
            assertThat(context).isNotNull();
            assertThat(context.userActionId()).isEqualTo(actionId);
            assertThat(context.correlationId()).isEqualTo(actionId);
            assertThat(context.httpMethod()).isEqualTo("POST");
            assertThat(context.routeTemplate()).isEqualTo("/api/episodes/{id}/close");
            ((MockHttpServletResponse) res).setStatus(204);
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER)).isNotEqualTo(
                "22222222-2222-2222-2222-222222222222");
        assertThat(UUID.fromString(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))).isNotNull();
        assertThat(response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER)).isEqualTo(actionId.toString());
        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(AuditRequestContext.current()).isNull();
    }

    @Test
    void invalidUserActionIdIsIgnoredAndRequestStillGetsBackendIds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients");
        request.addHeader(RequestCorrelationFilter.USER_ACTION_ID_HEADER, "not-a-uuid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            AuditRequestContext.Context context = AuditRequestContext.current();
            assertThat(context.userActionId()).isNull();
            assertThat(context.correlationId()).isEqualTo(context.requestId());
        });

        assertThat(UUID.fromString(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))).isNotNull();
        assertThat(response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER))
                .isEqualTo(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER));
        assertThat(AuditRequestContext.current()).isNull();
    }
}
