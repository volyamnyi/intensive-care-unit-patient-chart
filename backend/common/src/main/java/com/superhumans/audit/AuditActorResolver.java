package com.superhumans.audit;

import com.superhumans.entity.core.User;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Builds actor snapshots from trusted server-side security state. Never from client input. */
public final class AuditActorResolver {

    private AuditActorResolver() {
    }

    public static AuditEvent.AuditActor fromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
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

    /** Actor from the current request's trusted authentication, or UNKNOWN outside requests. */
    public static AuditEvent.AuditActor fromCurrentContext() {
        try {
            return fromAuthentication(SecurityContextHolder.getContext().getAuthentication());
        } catch (RuntimeException exception) {
            return unknown();
        }
    }

    /** Request actor when present, otherwise the given system service identity. */
    public static AuditEvent.AuditActor fromCurrentContextOrSystem(String serviceId) {
        AuditEvent.AuditActor actor = fromCurrentContext();
        return actor.type() == AuditEvent.ActorType.UNKNOWN ? system(serviceId) : actor;
    }

    public static AuditEvent.AuditActor unknown() {
        return new AuditEvent.AuditActor(
                AuditEvent.ActorType.UNKNOWN, null, null, null, Set.of(), null, null);
    }
}
