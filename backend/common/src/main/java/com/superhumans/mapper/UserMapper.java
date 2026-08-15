package com.superhumans.mapper;

import com.superhumans.dto.UserResponse;
import com.superhumans.entity.core.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User entity);
}
