package com.superhumans.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.superhumans.model.user.Role;
import com.superhumans.model.user.User;
import com.superhumans.service.impl.UserServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAuthenticationProvider {

    @Value("${security.jwt.secret-key}")
    String secretKey;

    @Value("${security.jwt.expiration}")
    Integer expiration;

    final UserServiceImpl userService;

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createToken(User user) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expiration);

        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        return JWT.create()
                .withSubject(user.getLogin())
                .withIssuedAt(now)
                .withExpiresAt(validity)
                .withClaim("firstName", user.getFirstName())
                .withClaim("lastName", user.getLastName())
                .withClaim("businessRole", user.getBusinessRole())
                .withClaim("userRole", String.valueOf(user.getUserRole()))
                .sign(algorithm);
    }

    public Authentication validateToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        JWTVerifier verifier = JWT.require(algorithm).build();

        DecodedJWT decoded = verifier.verify(token);

        User user = User.builder()
                .login(decoded.getSubject())
                .firstName(decoded.getClaim("firstName").asString())
                .lastName(decoded.getClaim("lastName").asString())
                .businessRole(decoded.getClaim("businessRole").asString())
                .userRole(Role.valueOf(decoded.getClaim("userRole").asString()))
                .build();

        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    public Authentication validateTokenStrongly(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        JWTVerifier verifier = JWT.require(algorithm).build();

        DecodedJWT decoded = verifier.verify(token);

        User user = userService.findByLogin(decoded.getSubject());

        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}
