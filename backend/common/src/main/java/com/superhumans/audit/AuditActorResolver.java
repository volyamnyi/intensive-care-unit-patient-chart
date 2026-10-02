package com.superhumans.audit;

import com.superhumans.entity.core.User;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Builds actor snapshots from trusted server-side security state. Never from client input. */
public final class AuditActorResolver {

    private AuditActorResolver() {
    }

    public static AuditEvent.AuditActor fromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return unknown();
        }
        Long id = authentication.getCredentials() instanceof Long userId ? userId : null;
        Set<String> roles = new TreeSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                roles.add(value.substring("ROLE_".length()));
            }
        }
        return new AuditEvent.AuditActor(
                AuditEvent.ActorType.USER,
                id == null ? null : id.toString(),
                authentication.getName(),
                null,
                roles,
                null,
                null);
    }

    public static AuditEvent.AuditActor fromUser(User user) {
        if (user == null) {
            return unknown();
        }
        return new AuditEvent.AuditActor(
                AuditEvent.ActorType.USER,
                user.getId() == null ? null : user.getId().toString(),
                user.getLogin(),
                user.getFullName(),
                user.getRole() == null ? Set.of() : Set.of(user.getRole().name()),
                null,
                null);
    }

    public static AuditEvent.AuditActor system(String serviceId) {
        return new AuditEvent.AuditActor(
                AuditEvent.ActorType.SYSTEM, serviceId, null, null, Set.of(), null, null);
    }

    public static AuditEvent.AuditActor unknown() {
        return new AuditEvent.AuditActor(
                AuditEvent.ActorType.UNKNOWN, null, null, null, Set.of(), null, null);
    }
}
