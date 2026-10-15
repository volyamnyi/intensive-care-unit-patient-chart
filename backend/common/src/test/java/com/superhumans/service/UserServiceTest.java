package com.superhumans.service;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.exception.BadRequestException;
import com.superhumans.repository.core.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Unit tests for the platform user-domain service (#424). */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    AuditService auditService;

    UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, auditService);
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setLogin("user" + id);
        user.setRole(role);
        return user;
    }

    @Test
    void updateRole_changesRoleAndReturnsPreviousRole() {
        User user = user(7L, UserRole.NURSE);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<UserService.RoleChange> result = userService.updateRole(7L, "DOCTOR", 1L);

        assertThat(result).isPresent();
        assertThat(result.get().previousRole()).isEqualTo(UserRole.NURSE);
        assertThat(result.get().user().getRole()).isEqualTo(UserRole.DOCTOR);
        assertThat(result.get().user().getUpdatedBy()).isEqualTo(1L);
        verify(auditService).logAction(eq("User"), isNull(), eq("ADMIN_UPDATE_ROLE:DOCTOR"), eq(1L));
    }

    @Test
    void updateRole_unknownRoleString_throwsBadRequest() {
        assertThatThrownBy(() -> userService.updateRole(7L, "WIZARD", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("WIZARD");
    }

    @Test
    void updateRole_blankRole_throwsBadRequest() {
        assertThatThrownBy(() -> userService.updateRole(7L, " ", 1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateRole_unknownUser_returnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.updateRole(99L, "DOCTOR", 1L)).isEmpty();
    }

    @Test
    void softDelete_marksDeletedAndAudits() {
        User user = user(8L, UserRole.NURSE);
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<User> result = userService.softDelete(8L, 1L);

        assertThat(result).isPresent();
        assertThat(result.get().getDeleted()).isTrue();
        assertThat(result.get().getUpdatedBy()).isEqualTo(1L);
        verify(auditService).logAction(eq("User"), isNull(), eq("ADMIN_DELETE_USER:soft-deleted"), eq(1L));
    }

    @Test
    void softDelete_selfDeletion_throwsBadRequest() {
        assertThatThrownBy(() -> userService.softDelete(1L, 1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void softDelete_unknownUser_returnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.softDelete(99L, 1L)).isEmpty();
    }

    @Test
    void stats_countsRolesFromAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, UserRole.DOCTOR),
                user(2L, UserRole.DOCTOR),
                user(3L, UserRole.NURSE),
                user(4L, UserRole.HEAD_OF_DEPARTMENT),
                user(5L, UserRole.ADMINISTRATOR),
                user(6L, UserRole.PROSTHETIST)));

        Map<String, Object> stats = userService.stats();

        assertThat(stats).containsEntry("totalUsers", 6)
                .containsEntry("doctors", 2L)
                .containsEntry("nurses", 1L)
                .containsEntry("headsOfDepartment", 1L)
                .containsEntry("administrators", 1L);
        verify(userRepository).findAll();
        // stats() is read-only: no audit rows.
        verifyNoInteractions(auditService);
    }

    @Test
    void parseRole_acceptsValidEnumName() {
        assertThat(userService.parseRole("HEAD_OF_DEPARTMENT")).isEqualTo(UserRole.HEAD_OF_DEPARTMENT);
    }
}
