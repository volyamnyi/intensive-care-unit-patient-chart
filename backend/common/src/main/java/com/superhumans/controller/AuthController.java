package com.superhumans.controller;

import com.superhumans.audit.AuditClientIpResolver;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.auth.TokenRevocationService;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.service.AuthService;
import com.superhumans.util.AuthUsers;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;
    JwtTokenProvider jwtTokenProvider;
    TokenRevocationService tokenRevocationService;
    AuditClientIpResolver clientIpResolver;

    // Cookie lifetime follows the token TTL (#371): a TTL reconfiguration
    // must not silently diverge cookie and token lifetimes.
    @Value("${app.jwt.expiration-ms:86400000}")
    @NonFinal
    long jwtExpirationMs = 86400000L;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        // #361: one trust path for the limiter and the audit trail. The resolver
        // honours the trusted-proxy CIDR gate instead of trusting whatever
        // getRemoteAddr() was rewritten to.
        String clientIp = clientIpResolver.resolveClientIp(request);
        ResponseEntity<LoginResponse> response = authService.login(req, clientIp);
        if (response.getBody() == null) {
            return response;
        }
        String token = jwtTokenProvider.generateToken(
                response.getBody().getLogin(),
                response.getBody().getRole(),
                response.getBody().getUserId());
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtExpirationMs / 1000)
                .build();
        LoginResponse loginResponse = response.getBody();
        loginResponse.setToken(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletRequest request) {
        String token = resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            tokenRevocationService.revoke(jwtTokenProvider.getJtiFromToken(token),
                    jwtTokenProvider.getExpirationFromToken(token).toInstant());
        }
        Long userId = AuthUsers.idOrNull(authentication);
        if (userId != null) {
            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                    .orElse(null);
            authService.logout(userId, role, request.getRemoteAddr());
        }
        ResponseCookie clearCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) return bearer.substring(7);
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }
}
