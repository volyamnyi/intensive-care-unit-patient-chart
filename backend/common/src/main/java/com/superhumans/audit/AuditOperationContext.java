package com.superhumans.audit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Tracks the current root audit operation per thread so multi-object use cases
 * emit one root event with child events linked by {@code parentAuditId}.
 * A retry of the same user intent keeps its own root: correlation comes from
 * {@link AuditRequestContext}, never from reusing this stack.
 */
public final class AuditOperationContext {

    private static final ThreadLocal<Deque<UUID>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private AuditOperationContext() {
    }

    public static Scope begin(UUID rootAuditId) {
        if (rootAuditId == null) {
            throw new IllegalArgumentException("Root audit ID is required");
        }
        STACK.get().push(rootAuditId);
        return new Scope();
    }

    public static UUID currentRootId() {
        return STACK.get().peek();
    }

    public static final class Scope implements AutoCloseable {
        private boolean closed;

        private Scope() {
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                Deque<UUID> stack = STACK.get();
                if (!stack.isEmpty()) {
                    stack.pop();
                }
                if (stack.isEmpty()) {
                    STACK.remove();
                }
            }
        }
    }
}
