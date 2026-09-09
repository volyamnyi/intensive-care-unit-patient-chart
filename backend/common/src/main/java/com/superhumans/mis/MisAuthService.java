package com.superhumans.mis;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Bearer token provider for the real MIS API.
 * <p>
 * The token is cached in memory and reused until {@code expires_in} minus the
 * configured skew; refreshes are single-flight (synchronized) so concurrent
 * callers share one token request. Callers must never log or expose the token —
 * this class logs HTTP status and latency only.
 * <p>
 * Token endpoint contract (confirmed by the owner):
 * {@code POST {base-url}{token-path}} with
 * {@code Content-Type: application/x-www-form-urlencoded} and fields
 * {@code grant_type=password}, {@code username={login}@@@{installationGuid}},
 * {@code password={password}} answers
 * {@code {access_token, token_type, expires_in, login, ...}} where
 * {@code expires_in} is seconds (default 3600 when absent — the real value is
 * ~86400).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MisAuthService {

    final RestTemplate restTemplate;
    final ObjectMapper objectMapper;
    final MisApiProperties properties;

    volatile String cachedToken;
    volatile long expiresAtMillis;

    /**
     * Returns a valid Bearer token, reusing the cached one until it expires.
     *
     * @return a non-blank access token, never null
     * @throws MisAuthException when authentication fails (no secrets in the message)
     */
    public String getAccessToken() {
        String token = cachedToken;
        if (token != null && System.currentTimeMillis() < expiresAtMillis) {
            return token;
        }
        synchronized (this) {
            token = cachedToken;
            if (token != null && System.currentTimeMillis() < expiresAtMillis) {
                return token;
            }
            return fetchToken();
        }
    }

    /**
     * Drops the cached token so the next {@link #getAccessToken()} re-authenticates.
     * Used after an HTTP 401 from the MIS API (single re-auth per call).
     */
    public synchronized void invalidateToken() {
        cachedToken = null;
        expiresAtMillis = 0;
    }

    private synchronized String fetchToken() {
        properties.ensureConfigured();
        String url = properties.getBaseUrl() + properties.getTokenPath();
        long started = System.currentTimeMillis();
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("username", properties.getLogin() + "@@@" + properties.getInstallationGuid());
            form.add("password", properties.getPassword());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String response = restTemplate.postForObject(
                    url, new HttpEntity<>(form, headers), String.class);
            JsonNode root = objectMapper.readTree(response);
            String token = firstText(root, "access_token", "accessToken", "token");
            long expiresInSec = firstLong(root, 3600L, "expires_in", "expiresIn");
            if (token == null || token.isBlank()) {
                throw new MisAuthException("MIS authentication failed: token missing in response");
            }
            cachedToken = token;
            long ttlSec = Math.max(0, expiresInSec - properties.getTokenSkewSec());
            expiresAtMillis = System.currentTimeMillis() + ttlSec * 1000L;
            log.debug("MIS token acquired, expiresIn={}s, latencyMs={}",
                    expiresInSec, System.currentTimeMillis() - started);
            return token;
        } catch (MisAuthException e) {
            invalidateToken();
            throw e;
        } catch (HttpStatusCodeException e) {
            invalidateToken();
            throw new MisAuthException(
                    "MIS authentication failed: httpStatus=" + e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            invalidateToken();
            throw new MisAuthException("MIS authentication failed: auth server unreachable", e);
        } catch (MisApiException e) {
            throw e;
        } catch (Exception e) {
            invalidateToken();
            throw new MisAuthException("MIS authentication failed: bad token response", e);
        }
    }

    private String firstText(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && node.isTextual()) {
                return node.asText();
            }
        }
        return null;
    }

    private long firstLong(JsonNode root, long fallback, String... names) {
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && node.isNumber()) {
                return node.asLong();
            }
        }
        return fallback;
    }
}
