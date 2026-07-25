package com.superhumans.service;

import com.superhumans.model.user.Credentials;
import com.superhumans.model.user.SignUp;
import com.superhumans.model.user.User;
import org.springframework.stereotype.Service;

import java.util.List;

//@Service
public interface UserService {
    User login(Credentials credentials);

    User register(SignUp user);

    User findByLogin(String login);

    List<User> getAllUsers();

    User updateUserById(SignUp userDto);

    void deleteUserById(Integer id);
}
