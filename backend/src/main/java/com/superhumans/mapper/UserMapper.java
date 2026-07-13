package com.superhumans.mapper;

import com.superhumans.dto.UserResponse;
import com.superhumans.entity.User;

public class UserMapper {

    public static UserResponse toResponse(User entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .login(entity.getLogin())
                .fullName(entity.getFullName())
                .role(entity.getRole())
                .email(entity.getEmail())
                .specialityCode(entity.getSpecialityCode())
                .specialityName(entity.getSpecialityName())
                .phone(entity.getPhone())
                .build();
    }
}
