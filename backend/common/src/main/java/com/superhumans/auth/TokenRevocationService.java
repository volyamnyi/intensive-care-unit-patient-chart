package com.superhumans.auth;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** In-memory revocation window for stateless access tokens. */
@Service
public class TokenRevocationService {

    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId != null && expiresAt != null && expiresAt.isAfter(Instant.now())) {
            revokedTokens.put(tokenId, expiresAt);
        }
    }

    public boolean isRevoked(String tokenId) {
        if (tokenId == null) return true;
        Instant expiresAt = revokedTokens.get(tokenId);
        if (expiresAt == null) return false;
        if (expiresAt.isAfter(Instant.now())) return true;
        revokedTokens.remove(tokenId, expiresAt);
        return false;
    }
}
