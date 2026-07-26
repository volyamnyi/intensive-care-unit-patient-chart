package com.superhumans.repository;

import com.superhumans.entity.User;
import com.superhumans.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    List<User> findByRole(UserRole role);
    boolean existsByLogin(String login);
    List<User> findAllByOrderByIdAsc();
}
