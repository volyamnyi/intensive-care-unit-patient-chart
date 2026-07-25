package com.superhumans.config;

import com.superhumans.service.impl.UserServiceImpl;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.superhumans.model.user.Permission.*;
import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    UserAuthenticationEntryPoint userAuthenticationEntryPoint;
    UserAuthenticationProvider userAuthenticationProvider;
    PasswordEncoder passwordEncoder;
    UserServiceImpl userService;
    //ActiveDirectoryLdapAuthenticationProvider ldapAuthenticationProvider;



    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider dao = new DaoAuthenticationProvider();
        dao.setUserDetailsService(userService);
        dao.setPasswordEncoder(passwordEncoder);
        return dao;
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(userAuthenticationProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .exceptionHandling(c -> c.authenticationEntryPoint(userAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> req

                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").hasAnyAuthority(ADMIN_CREATE.getPermission())

                        .requestMatchers(GET, "/api/medicinelist/**")
                        .hasAnyAuthority(EMPLOYEE_READ.getPermission(), ADMIN_READ.getPermission())

                        .requestMatchers(POST, "/api/medicinelist/**")
                        .hasAnyAuthority(EMPLOYEE_CREATE.getPermission(), ADMIN_CREATE.getPermission())

                        .requestMatchers(PUT, "/api/medicinelist/**")
                        .hasAnyAuthority(EMPLOYEE_UPDATE.getPermission(), ADMIN_UPDATE.getPermission())

                        .requestMatchers(DELETE, "/api/medicinelist/**")
                        .hasAnyAuthority(EMPLOYEE_DELETE.getPermission(), ADMIN_DELETE.getPermission())

                        .requestMatchers(GET, "/api/**").hasAuthority(ADMIN_READ.getPermission())
                        .requestMatchers(POST, "/api/**").hasAuthority(ADMIN_CREATE.getPermission())
                        .requestMatchers(PUT, "/api/**").hasAuthority(ADMIN_UPDATE.getPermission())
                        .requestMatchers(DELETE, "/api/**").hasAuthority(ADMIN_DELETE.getPermission())

                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
