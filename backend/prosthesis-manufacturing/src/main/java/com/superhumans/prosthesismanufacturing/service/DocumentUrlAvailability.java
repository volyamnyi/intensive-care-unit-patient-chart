package com.superhumans.prosthesismanufacturing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Liveness probe for MIS-hosted document URLs
 * ({@code spiDocumentProsthesCheck} → {@code documentUrl}).
 * <p>
 * Contract (setup flow, order step): a document whose URL answers
 * <b>404</b> is excluded from the selection. Every other outcome —
 * success, redirect, another status, a server that rejects {@code HEAD},
 * a timeout or an unreachable host — keeps the document (fail-open):
 * only a confirmed 404 proves the document is gone, while anything else
 * may be transient (or a portal that simply dislikes probes).
 */
@Slf4j
@Component
public class DocumentUrlAvailability {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestTemplate restTemplate;

    public DocumentUrlAvailability() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        this.restTemplate = new RestTemplate(factory);
    }

    DocumentUrlAvailability(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Returns {@code false} only when the URL answers HTTP 404.
     */
    public boolean isAvailable(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            restTemplate.execute(url, HttpMethod.HEAD, null, null);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("MIS document URL answers 404, excluding: {}", redact(url));
            return false;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED
                    || e.getStatusCode() == HttpStatus.NOT_IMPLEMENTED) {
                return getFallback(url);
            }
            return true;
        } catch (HttpServerErrorException | ResourceAccessException e) {
            return true;
        } catch (Exception e) {
            log.debug("MIS document URL probe failed ({}), keeping: {}",
                    e.getClass().getSimpleName(), redact(url));
            return true;
        }
    }

    private boolean getFallback(String url) {
        try {
            restTemplate.execute(url, HttpMethod.GET, null, null);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("MIS document URL answers 404 on GET fallback, excluding: {}", redact(url));
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static String redact(String url) {
        int query = url.indexOf('?');
        return query < 0 ? url : url.substring(0, query) + "?<redacted>";
    }
}
