package com.superhumans.mis;

import java.net.SocketTimeoutException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * Generic REST client for MIS API.
 * <p>
 * Dual-mode ({@code app.mis.mode}, #255):
 * <ul>
 *   <li>{@code wiremock} (default) — legacy behavior: {@code POST {wiremock-url}/api/run}
 *       with the static integration login; no Bearer token.</li>
 *   <li>{@code real} — {@code POST {api.base-url}{api.run-path}} with
 *       {@code Authorization: Bearer} from {@link MisAuthService} and the API
 *       identity from {@code app.mis.api.*}; a single 401 triggers one token
 *       refresh and one retry.</li>
 * </ul>
 * <p>
 * <b>POLICY: ICU Chart is READ-ONLY client of MIS.</b>
 * Only read-methods (procedure Search/Details/Dictionary families) are allowed.
 * Write methods (Save/Create/Update/Delete families) MUST NEVER be called via this client.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MisApiClient implements InitializingBean {

    final RestTemplate restTemplate;
    final ObjectMapper objectMapper;

    @Value("${app.mis.wiremock-url:http://localhost:9090}")
    String misBaseUrl;

    @Value("${app.mis.installation-guid:00000000-0000-0000-0000-000000000000}")
    String installationGuid;

    @Value("${app.mis.login:integration}")
    String login;

    @Value("${app.mis.mode:wiremock}")
    String mode;

    @Autowired(required = false)
    @Setter
    MisApiProperties properties;

    @Autowired(required = false)
    @Setter
    MisAuthService misAuthService;

    @Override
    public void afterPropertiesSet() {
        if (!"wiremock".equalsIgnoreCase(mode) && !"real".equalsIgnoreCase(mode)) {
            throw new IllegalStateException(
                    "Unknown app.mis.mode=" + mode + ", expected wiremock|real");
        }
        if (isRealMode()) {
            if (properties == null) {
                throw new IllegalStateException(
                        "Real MIS mode requires the MisApiProperties bean");
            }
            properties.assertRealModeReady();
            if (misAuthService == null) {
                throw new IllegalStateException(
                        "Real MIS mode requires the MisAuthService bean");
            }
        }
    }

    public JsonNode callMethod(String methodName, Param... params) {
        boolean real = isRealMode();
        String loginValue = real ? properties.getLogin() : login;
        String installationValue = real ? properties.getInstallationGuid() : installationGuid;

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
        loginParam.put("value", loginValue);
        paramsArray.add(loginParam);

        requestBody.set("params", paramsArray);
        requestBody.put("installationId", installationValue);

        if (!real) {
            String url = misBaseUrl + "/api/run";
            log.debug("Calling MIS API: {} method={}", url, methodName);
            return post(url, requestBody, null, methodName);
        }

        String url = properties.getBaseUrl() + properties.getRunPath();
        log.debug("Calling MIS API: {} method={}", url, methodName);
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

    private boolean isRealMode() {
        return "real".equalsIgnoreCase(mode);
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
