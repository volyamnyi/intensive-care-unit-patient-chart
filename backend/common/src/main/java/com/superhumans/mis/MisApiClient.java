package com.superhumans.mis;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Generic REST client for MIS API.
 * All calls go to POST /api/run with method name + parameters.
 * <p>
 * <b>POLICY: ICU Chart is READ-ONLY client of MIS.</b>
 * Only read-methods (spzIB*Search, spzIB*Details, spzIB*Dictionary) are allowed.
 * Write methods (spzIB*Save, spzIB*Create, etc.) MUST NEVER be called via this client.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MisApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.mis.wiremock-url:http://localhost:9090}")
    private String misBaseUrl;

    @Value("${app.mis.installation-guid:00000000-0000-0000-0000-000000000000}")
    private String installationGuid;

    @Value("${app.mis.login:integration}")
    private String login;

    public JsonNode callMethod(String methodName, Param... params) {
        try {
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
            loginParam.put("value", login);
            paramsArray.add(loginParam);

            requestBody.set("params", paramsArray);
            requestBody.put("installationId", installationGuid);

            String url = misBaseUrl + "/api/run";
            log.debug("Calling MIS API: {} method={}", url, methodName);

            String response = restTemplate.postForObject(url, requestBody, String.class);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("MIS API call failed: method={}, error={}", methodName, e.getMessage());
            throw new RuntimeException("MIS API call failed: " + methodName, e);
        }
    }

    public record Param(String name, String value) {}
}
