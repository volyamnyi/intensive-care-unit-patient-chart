package com.superhumans.service.impl;

import com.superhumans.exception.AppException;
import com.superhumans.model.user.*;
import com.superhumans.repository.UserRepository;
import com.superhumans.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.ldap.core.DirContextOperations;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LdapAuthenticationProvider ldapAuthenticationProvider;

    private final LdapAuthService ldapAuthService;


    public User login(Credentials credentials) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        credentials.login(),
                        credentials.password()
                );

        Authentication auth;
        try {
            auth = ldapAuthenticationProvider.authenticate(token);
        } catch (Exception e) {
            throw new AppException("Invalid AD credentials", HttpStatus.UNAUTHORIZED);
        }

        LdapUserDetailsAdapter ldapUser = (LdapUserDetailsAdapter) auth.getPrincipal();

        String login = ldapUser.getUsername();


        return userRepository.findByLogin(login).orElseGet(() -> {
            User newUser = new User();
            newUser.setLogin(ldapUser.getUsername());
            newUser.setFirstName(ldapUser.getFirstName());
            newUser.setLastName(ldapUser.getLastName());
            newUser.setUserRole(Role.EMPLOYEE);
            newUser.setPassword("LDAP");

            return userRepository.save(newUser);
        });
    }



    public User register(SignUp userDto) {
        Optional<User> optionalUser = userRepository.findByLogin(userDto.login());

        if (optionalUser.isPresent()) {
            throw new AppException("Login already exists", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setFirstName(userDto.firstName());
        user.setLastName(userDto.lastName());
        user.setMiddleName(userDto.middleName());
        user.setLogin(userDto.login());
        user.setBusinessRole(userDto.businessRole());
        user.setUserRole(Role.valueOf(userDto.userRole()));
        user.setPassword(passwordEncoder.encode(CharBuffer.wrap(userDto.password())));

        return userRepository.save(user);
    }


    public User findByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new AppException("Unknown user", HttpStatus.NOT_FOUND));
    }


    public List<User> getAllUsers() {
        return userRepository.getAllUsers();
    }


    public User updateUserById(SignUp userDto) {
        Optional<User> optionalUser = userRepository.findByLogin(userDto.login());

        if (optionalUser.isEmpty()) {
            throw new AppException("User not found", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setId(userDto.id());
        user.setFirstName(userDto.firstName());
        user.setLastName(userDto.lastName());
        user.setMiddleName(userDto.middleName());
        user.setLogin(userDto.login());
        user.setBusinessRole(userDto.businessRole());
        user.setUserRole(Role.valueOf(userDto.userRole()));
        user.setPassword(passwordEncoder.encode(CharBuffer.wrap(userDto.password())));

        return userRepository.updateUserById(user);
    }


    public void deleteUserById(Integer id) {
        userRepository.deleteUserById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }
}
