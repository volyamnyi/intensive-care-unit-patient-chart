package com.superhumans.audit;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Request-scoped correlation data shared by backend filters and domain event writers. */
public final class AuditRequestContext {

    public static final String REQUEST_ATTRIBUTE = AuditRequestContext.class.getName() + ".context";
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private AuditRequestContext() {
    }

    public static Context current() {
        return CURRENT.get();
    }

    public static Scope install(Context context) {
        Context previous = CURRENT.get();
        CURRENT.set(context);
        return new Scope(previous);
    }

    public static final class Context {

        private final UUID requestId;
        private final UUID userActionId;
        private final UUID correlationId;
        private final long startedAtNanos;
        private final String httpMethod;
        private final String clientIp;
        private volatile String routeTemplate;

        public Context(UUID requestId, UUID userActionId, UUID correlationId,
                       long startedAtNanos, String httpMethod) {
            this(requestId, userActionId, correlationId, startedAtNanos, httpMethod, null);
        }

        public Context(UUID requestId, UUID userActionId, UUID correlationId,
                       long startedAtNanos, String httpMethod, String clientIp) {
            if (requestId == null || correlationId == null) {
                throw new IllegalArgumentException("Request and correlation IDs are required");
            }
            this.requestId = requestId;
            this.userActionId = userActionId;
            this.correlationId = correlationId;
            this.startedAtNanos = startedAtNanos;
            this.httpMethod = httpMethod;
            this.clientIp = clientIp;
        }

        public UUID requestId() {
            return requestId;
        }

        public UUID userActionId() {
            return userActionId;
        }

        public UUID correlationId() {
            return correlationId;
        }

        public long startedAtNanos() {
            return startedAtNanos;
        }

        public String httpMethod() {
            return httpMethod;
        }

        public String clientIp() {
            return clientIp;
        }

        public String routeTemplate() {
            return routeTemplate;
        }

        public void setRouteTemplate(String routeTemplate) {
            this.routeTemplate = routeTemplate;
        }
    }

    public static final class Scope implements AutoCloseable {
        private final Context previous;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Scope(Context previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                if (previous == null) {
                    CURRENT.remove();
                } else {
                    CURRENT.set(previous);
                }
            }
        }
    }
}
