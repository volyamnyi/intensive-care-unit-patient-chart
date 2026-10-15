package com.superhumans.controller;

import com.superhumans.entity.core.AuthProvider;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.mapper.UserMapper;
import com.superhumans.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JSON contract of the user endpoints (#424): responses are
 * {@link com.superhumans.dto.UserResponse} — no {@code authProvider}, no
 * legacy {@code permissions} CSV, no JPA audit/version fields leak.
 */
class UserResponseContractTest {

    UserService userService;
    UserMapper userMapper = Mappers.getMapper(UserMapper.class);
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, userMapper, null))
                .build();
    }

    private User user() {
        User user = new User();
        user.setId(5L);
        user.setLogin("nurse1");
        user.setFullName("Олена Ткаченко");
        user.setRole(UserRole.NURSE);
        user.setEmail("n@example.com");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setDeleted(false);
        return user;
    }

    @Test
    void getMe_mapsAuthenticatedLogin() throws Exception {
        when(userService.findByLogin("nurse1")).thenReturn(Optional.of(user()));

        mockMvc.perform(get("/api/users/me")
                        .principal(new TestingAuthenticationToken("nurse1", "n")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.login").value("nurse1"))
                .andExpect(jsonPath("$.role").value("NURSE"))
                .andExpect(jsonPath("$.deleted").value(false))
                .andExpect(jsonPath("$.authProvider").doesNotExist())
                .andExpect(jsonPath("$.permissions").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.createdBy").doesNotExist());
    }

    @Test
    void getDoctors_mapsListWithoutEntityLeak() throws Exception {
        when(userService.findByRole(UserRole.DOCTOR)).thenReturn(List.of(user()));

        mockMvc.perform(get("/api/users/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("nurse1"))
                .andExpect(jsonPath("$[0].authProvider").doesNotExist())
                .andExpect(jsonPath("$[0].permissions").doesNotExist());
    }
}
