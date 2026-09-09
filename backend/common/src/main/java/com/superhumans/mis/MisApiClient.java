package com.superhumans.mis;

import java.net.SocketTimeoutException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Generic REST client for the real MIS API.
 * <p>
 * Every call is {@code POST {base-url}{run-path}} with an
 * {@code Authorization: Bearer} token from {@link MisAuthService} and the API
 * identity ({@code login} param, {@code installationId}) from
 * {@link MisApiProperties}. A single HTTP 401 triggers one token
 * invalidation + re-fetch and one retry.
 * <p>
 * <b>POLICY: ICU Chart is READ-ONLY client of MIS.</b>
 * Only read-methods (procedure Search/Details/Dictionary families) are allowed.
 * Write methods (Save/Create/Update/Delete families) MUST NEVER be called via this client.
 * The sole allowed write is {@code sendPdf} which transfers an immutable PDF.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MisApiClient {

    final RestTemplate restTemplate;
    final ObjectMapper objectMapper;
    final MisAuthService misAuthService;
    final MisApiProperties properties;

    public JsonNode callMethod(String methodName, Param... params) {
        properties.ensureConfigured();
        String url = properties.getBaseUrl() + properties.getRunPath();
        log.debug("Calling MIS API: {} method={}", url, methodName);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("name", methodName);

        ArrayNode paramsArray = objectMapper.createArrayNode();
        for (Param param : params) {
            ObjectNode paramNode = objectMapper.createObjectNode();
            paramNode.put("name", param.name);
            paramNode.put("value", param.value);
            paramsArray.add(paramNode);
        }

        ObjectNode loginParam = objectMapper.createObjectNode();
        loginParam.put("name", "Login");
        loginParam.put("value", properties.getLogin());
        paramsArray.add(loginParam);

        requestBody.set("params", paramsArray);
        requestBody.put("installationId", properties.getInstallationGuid());

        try {
            return post(url, requestBody, misAuthService.getAccessToken(), methodName);
        } catch (MisBadResponseException e) {
            if (e.getStatusCode() == 401) {
                misAuthService.invalidateToken();
                return post(url, requestBody, misAuthService.getAccessToken(), methodName);
            }
            throw e;
        }
    }

    private JsonNode post(String url, ObjectNode requestBody, String bearerToken,
            String methodName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerToken != null) {
                headers.setBearerAuth(bearerToken);
            }
            String response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(requestBody.toString(), headers),
                    String.class).getBody();
            return objectMapper.readTree(response);
        } catch (HttpStatusCodeException e) {
            log.error("MIS API call failed: method={}, httpStatus={}",
                    methodName, e.getStatusCode().value());
            throw new MisBadResponseException(methodName, e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            log.error("MIS API call failed: method={}, error={}", methodName, e.getMessage());
            if (hasCause(e, SocketTimeoutException.class)) {
                throw new MisTimeoutException("MIS API call timed out: " + methodName, e);
            }
            throw new MisApiException("MIS API call failed: " + methodName, e);
        } catch (MisApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("MIS API call failed: method={}, error={}", methodName, e.getMessage());
            throw new MisApiException("MIS API call failed: " + methodName, e);
        }
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public record Param(String name, String value) {}
}
