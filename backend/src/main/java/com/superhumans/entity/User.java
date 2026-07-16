package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    String login;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
    String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    UserRole role;

    @Column(length = 100)
    String email;

    @Column(name = "speciality_code", length = 20)
    String specialityCode;

    @Column(name = "speciality_name", length = 200)
    String specialityName;

    @Column(length = 20)
    String phone;
}
