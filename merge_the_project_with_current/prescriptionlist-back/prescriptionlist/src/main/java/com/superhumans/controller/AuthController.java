package com.superhumans.controller;


import com.superhumans.config.UserAuthenticationProvider;
import com.superhumans.model.user.Credentials;
import com.superhumans.model.user.SignUp;
import com.superhumans.model.user.User;
import com.superhumans.service.UserService;
import com.superhumans.service.impl.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;


@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173","http://192.168.24.32:5173"}, allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.GET})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    UserServiceImpl userService;

    UserAuthenticationProvider userAuthenticationProvider;

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody @Valid Credentials credentials) {
        User user = userService.login(credentials);
        user.setToken(userAuthenticationProvider.createToken(user));
        user.setPassword(null);
        //user.setUserRole(null);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login/2P")
    public ResponseEntity<User> login2P(@RequestBody @Valid Credentials credentials) {
        User user = userService.login(credentials);
        user.setToken(userAuthenticationProvider.createToken(user));
        user.setPassword(null);
        //user.setUserRole(null);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/admin/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<User> register(@RequestBody @Valid SignUp user) {
        User createdUser = userService.register(user);
        createdUser.setToken(userAuthenticationProvider.createToken(createdUser));
        return ResponseEntity.created(URI.create("/users/" + createdUser.getId())).body(createdUser);
    }

    @GetMapping("/admin")
    @ResponseStatus(HttpStatus.OK)
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/admin")
    @ResponseStatus(HttpStatus.OK)
    public User updateUserById(@RequestBody @Valid SignUp userDto) {
        return userService.updateUserById(userDto);
    }

    @DeleteMapping("/admin/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteUserById(@PathVariable Integer id) {
        userService.deleteUserById(id);
    }

}
