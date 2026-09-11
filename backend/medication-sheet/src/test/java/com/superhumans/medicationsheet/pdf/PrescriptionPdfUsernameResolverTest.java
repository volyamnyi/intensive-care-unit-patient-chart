package com.superhumans.medicationsheet.pdf;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionPdfUsernameResolverTest {

    private static final PrescriptionPdfUsernameResolver RESOLVER = new PrescriptionPdfUsernameResolver();

    private static User user(Long id, String login, UserRole role) {
        return User.builder().id(id).login(login).fullName(login + " Full").role(role).build();
    }

    private static UUID nameUuid(String login) {
        return UUID.nameUUIDFromBytes(login.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static PrescriptionPdfSnapshot.Execution execution(String executorLogin, LocalDateTime at) {
        return new PrescriptionPdfSnapshot.Execution(
                nameUuid(executorLogin), at, "5", "Completed", null, null);
    }

    @Test
    void executedByResolvesToExecutorLogin() {
        Map<UUID, String> index = RESOLVER.buildLoginIndex(List.of(
                user(1L, "nurse1", UserRole.NURSE), user(2L, "nurse2", UserRole.NURSE)));

        List<String> logins = RESOLVER.resolveExecutionLogins(
                List.of(execution("nurse2", LocalDateTime.of(2026, 7, 25, 9, 0))), index);

        assertThat(logins).containsExactly("nurse2");
    }

    @Test
    void secondPersonIdNeverAppearsAmongNurses() {
        Map<UUID, String> index = RESOLVER.buildLoginIndex(List.of(
                user(1L, "nurse1", UserRole.NURSE), user(2L, "nurse2", UserRole.NURSE)));
        PrescriptionPdfSnapshot.Execution execution = new PrescriptionPdfSnapshot.Execution(
                nameUuid("nurse1"), LocalDateTime.of(2026, 7, 25, 9, 0),
                "5", "Completed", nameUuid("nurse2"), null);

        List<String> logins = RESOLVER.resolveExecutionLogins(List.of(execution), index);

        assertThat(logins).containsExactly("nurse1");
        assertThat(logins).doesNotContain("nurse2");
    }

    @Test
    void multipleNursesDeduplicatedAndOrderedByExecutedAt() {
        Map<UUID, String> index = RESOLVER.buildLoginIndex(List.of(
                user(1L, "nurse1", UserRole.NURSE), user(2L, "nurse2", UserRole.NURSE)));
        List<PrescriptionPdfSnapshot.Execution> executions = List.of(
                execution("nurse2", LocalDateTime.of(2026, 7, 25, 20, 0)),
                execution("nurse1", LocalDateTime.of(2026, 7, 25, 9, 0)),
                execution("nurse2", LocalDateTime.of(2026, 7, 25, 10, 0)));

        List<String> logins = RESOLVER.resolveExecutionLogins(executions, index);

        assertThat(logins).containsExactly("nurse1", "nurse2");
    }

    @Test
    void unknownExecutorResolvesToEmpty() {
        Map<UUID, String> index = RESOLVER.buildLoginIndex(List.of(user(1L, "nurse1", UserRole.NURSE)));

        List<String> logins = RESOLVER.resolveExecutionLogins(
                List.of(execution("ghost", LocalDateTime.of(2026, 7, 25, 9, 0))), index);

        assertThat(logins).isEmpty();
        assertThat(RESOLVER.resolveLogin(UUID.randomUUID(), index)).isEmpty();
        assertThat(RESOLVER.resolveLogin(null, index)).isEmpty();
    }

    @Test
    void attendingDoctorIsFirstDoctorByIdAndNeverFromDisplayName() {
        List<User> users = List.of(
                user(5L, "nurse1", UserRole.NURSE),
                user(3L, "doctor2", UserRole.DOCTOR),
                user(2L, "doctor1", UserRole.DOCTOR));

        assertThat(RESOLVER.resolveAttendingDoctor(users)).isEqualTo("doctor1");
    }

    @Test
    void attendingDoctorEmptyWhenNoDoctor() {
        List<User> users = List.of(user(5L, "nurse1", UserRole.NURSE));

        assertThat(RESOLVER.resolveAttendingDoctor(users)).isEmpty();
    }

    @Test
    void plannerLoginMustNotLeakIntoSignatures() {
        // The planner's own identity (who pressed "plan") is not an input of
        // the signature model at all: doctor comes from the fixed rule,
        // nurses only from executedBy.
        Map<UUID, String> index = RESOLVER.buildLoginIndex(List.of(
                user(1L, "planner", UserRole.DOCTOR), user(2L, "nurse1", UserRole.NURSE)));

        List<String> logins = RESOLVER.resolveExecutionLogins(List.of(), index);

        assertThat(logins).isEmpty();
        assertThat(RESOLVER.resolveAttendingDoctor(
                        List.of(user(1L, "planner", UserRole.DOCTOR))))
                .isEqualTo("planner");
    }
}
