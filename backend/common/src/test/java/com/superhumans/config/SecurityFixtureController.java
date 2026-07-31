package com.superhumans.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controller that mirrors the URL surface exercised by the generic
 * {@link SecurityConfig} unit tests. It is registered explicitly through
 * {@code @WebMvcTest(SecurityFixtureController.class)}, so it shadows paths
 * that the real application would serve (auth, swagger, api-docs).
 */
@RestController
public class SecurityFixtureController {

    @PostMapping("/api/auth/login")
    public ResponseEntity<Void> login() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/auth/restricted")
    public ResponseEntity<Void> restrictedAuthPath() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/private")
    public ResponseEntity<Void> privateEndpoint() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/admin-only")
    public ResponseEntity<Void> adminOnly() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/doctor-only")
    public ResponseEntity<Void> doctorOnly() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/swagger-ui/index.html")
    public ResponseEntity<Void> swaggerUi() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api-docs")
    public ResponseEntity<Void> apiDocs() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/v3/api-docs")
    public ResponseEntity<Void> v3ApiDocs() {
        return ResponseEntity.ok().build();
    }
}
