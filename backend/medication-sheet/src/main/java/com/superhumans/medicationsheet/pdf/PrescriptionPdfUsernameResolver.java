package com.superhumans.medicationsheet.pdf;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

/**
 * Resolves signature usernames for Form №003-4/о.
 *
 * <p>Codebase facts (must not regress):
 * <ul>
 *   <li>{@code PrescriptionDayPart.doctorName}/{@code nurseName} hold
 *   {@code UUID.nameUUIDFromBytes(login)} strings (or {@code "login/2P:login"}
 *   after {@code execute()}) — never printable raw;</li>
 *   <li>{@code PrescriptionExecution.executedBy}/{@code secondPersonId} are
 *   the same name-based UUIDs, <b>not</b> {@code users.id};</li>
 *   <li>the only authoritative login source is {@code users.login}.</li>
 * </ul>
 * Every unknown value resolves to {@code ""} (fail-closed); the renderer
 * receives resolved strings only.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrescriptionPdfUsernameResolver {

    /**
     * Builds the reverse index {@code nameUUID(login) → login} over the
     * given users. Null/blank logins are skipped.
     */
    public Map<UUID, String> buildLoginIndex(List<User> users) {
        Map<UUID, String> index = new LinkedHashMap<>();
        for (User user : users) {
            if (user == null || user.getLogin() == null || user.getLogin().isBlank()) {
                continue;
            }
            index.putIfAbsent(nameUuid(user.getLogin()), user.getLogin());
        }
        return index;
    }

    /** Resolves one name-based UUID to a login, or {@code ""} when unknown. */
    public String resolveLogin(UUID nameUuid, Map<UUID, String> loginIndex) {
        if (nameUuid == null) {
            return "";
        }
        return loginIndex.getOrDefault(nameUuid, "");
    }

    /**
     * Attending-doctor rule (Phase 17, fixed until the model gains an
     * explicit link): the first {@code DOCTOR} user ordered by id ascending.
     * Never derived from a display name / ПІБ, never invented — {@code ""}
     * when no doctor exists.
     */
    public String resolveAttendingDoctor(List<User> users) {
        return users.stream()
                .filter(user -> user != null && user.getRole() == UserRole.DOCTOR)
                .sorted(Comparator.comparing(User::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(User::getLogin)
                .filter(login -> login != null && !login.isBlank())
                .findFirst()
                .orElse("");
    }

    /**
     * Logins of the actual executors ({@code executedBy} only), deduplicated,
     * ordered by {@code executedAt} (nulls last). {@code secondPersonId} is
     * never consulted and can never appear here.
     */
    public List<String> resolveExecutionLogins(
            List<PrescriptionPdfSnapshot.Execution> executions, Map<UUID, String> loginIndex) {
        List<PrescriptionPdfSnapshot.Execution> ordered = new ArrayList<>(executions);
        ordered.sort(Comparator.comparing(
                PrescriptionPdfSnapshot.Execution::executedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        LinkedHashSet<String> logins = new LinkedHashSet<>();
        for (PrescriptionPdfSnapshot.Execution execution : ordered) {
            String login = resolveLogin(execution.executedBy(), loginIndex);
            if (!login.isEmpty()) {
                logins.add(login);
            }
        }
        return List.copyOf(logins);
    }

    static UUID nameUuid(String login) {
        return UUID.nameUUIDFromBytes(login.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
