package com.superhumans.service;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.exception.BadRequestException;
import com.superhumans.repository.core.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Platform user-domain service (#424): role changes, soft delete, stats and
 * the read operations the user/admin controllers need. Business rules (role
 * parsing, self-deletion guard, legacy audit emission) live here instead of
 * the web layer, and every mutation commits atomically with its audit row.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    AuditService auditService;

    /** Role change outcome: the updated user plus the role it replaced. */
    public record RoleChange(User user, UserRole previousRole) {
    }

    @Transactional(readOnly = true)
    public List<User> findAllOrdered() {
        return userRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    @Transactional(readOnly = true)
    public List<User> findByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    /**
     * Change a user's role. Returns empty when the user does not exist;
     * throws {@link BadRequestException} for an unknown role string. The
     * mutation and its legacy audit row commit in one transaction.
     */
    @Transactional
    public Optional<RoleChange> updateRole(Long id, String roleValue, Long actingUserId) {
        UserRole newRole = parseRole(roleValue);
        return userRepository.findById(id).map(user -> {
            // Admin self-service guards (#371 item 6): no self-demotion and no
            // lockout by demoting the last active administrator (recovery
            // would be direct DB access).
            if (actingUserId != null && actingUserId.equals(id)) {
                throw new BadRequestException("Неможливо змінити власну роль");
            }
            if (user.getRole() == UserRole.ADMINISTRATOR && newRole != UserRole.ADMINISTRATOR
                    && isLastActiveAdmin(id)) {
                throw new BadRequestException("Неможливо понизити останнього активного адміністратора");
            }
            UserRole previousRole = user.getRole();
            user.setRole(newRole);
            user.setUpdatedBy(actingUserId);
            userRepository.save(user);
            auditService.logAction("User", null, "ADMIN_UPDATE_ROLE:" + newRole.name(), actingUserId);
            return new RoleChange(user, previousRole);
        });
    }

    private boolean isLastActiveAdmin(Long excludedId) {
        return userRepository.findByRole(UserRole.ADMINISTRATOR).stream()
                .noneMatch(u -> !u.getId().equals(excludedId) && !Boolean.TRUE.equals(u.getDeleted()));
    }

    /**
     * Soft-delete a user account. Self-deletion is rejected; returns empty
     * when the user does not exist.
     */
    @Transactional
    public Optional<User> softDelete(Long id, Long actingUserId) {
        if (actingUserId != null && actingUserId.equals(id)) {
            throw new BadRequestException("Неможливо видалити власний обліковий запис");
        }
        return userRepository.findById(id).map(user -> {
            user.setDeleted(true);
            user.setUpdatedBy(actingUserId);
            userRepository.save(user);
            auditService.logAction("User", null, "ADMIN_DELETE_USER:soft-deleted", actingUserId);
            return user;
        });
    }

    /** In-memory role counts for the admin stats card. */
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        List<User> all = userRepository.findAll();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", all.size());
        stats.put("doctors", countByRole(all, UserRole.DOCTOR));
        stats.put("nurses", countByRole(all, UserRole.NURSE));
        stats.put("headsOfDepartment", countByRole(all, UserRole.HEAD_OF_DEPARTMENT));
        stats.put("administrators", countByRole(all, UserRole.ADMINISTRATOR));
        return stats;
    }

    /** Role-string validation shared by the user and permission endpoints. */
    public UserRole parseRole(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Роль обов'язкова");
        }
        try {
            return UserRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Невідома роль: " + value);
        }
    }

    private long countByRole(List<User> users, UserRole role) {
        return users.stream().filter(u -> u.getRole() == role).count();
    }
}
