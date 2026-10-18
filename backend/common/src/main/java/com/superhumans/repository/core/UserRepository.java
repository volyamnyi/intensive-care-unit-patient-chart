package com.superhumans.repository.core;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    boolean existsByLogin(String login);
    List<User> findAllByOrderByIdAsc();

    /**
     * #362: active users of a role only. Every caller wants live people -
     * sign-off pickers, department counts, brak-notification recipients, and the
     * last-admin guard all break if disabled accounts are counted or offered.
     * The admin user table lists everyone through findAllByOrderByIdAsc.
     */
    @Query("select u from User u where u.role = :role and coalesce(u.deleted, false) = false")
    List<User> findByRole(@Param("role") UserRole role);
}
