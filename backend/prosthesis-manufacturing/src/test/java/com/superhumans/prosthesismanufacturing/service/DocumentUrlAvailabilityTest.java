package com.superhumans.prosthesismanufacturing.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DocumentUrlAvailability} against a real local HTTP server:
 * only a confirmed 404 excludes, everything else keeps (fail-open).
 * No Spring context, no external network.
 */
class DocumentUrlAvailabilityTest {

    private HttpServer server;
    private String baseUrl;
    private DocumentUrlAvailability availability;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        }));
        server.createContext("/alive", e -> send(e, 200));
        server.createContext("/gone", e -> send(e, 404));
        server.createContext("/broken", e -> send(e, 500));
        server.createContext("/no-head", e -> {
            if ("HEAD".equalsIgnoreCase(e.getRequestMethod())) {
                send(e, 405);
            } else {
                send(e, 200);
            }
        });
        server.createContext("/no-head-gone", e -> {
            if ("HEAD".equalsIgnoreCase(e.getRequestMethod())) {
                send(e, 405);
            } else {
                send(e, 404);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        availability = new DocumentUrlAvailability(new RestTemplate(factory));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void okUrl_isAvailable() {
        assertThat(availability.isAvailable(baseUrl + "/alive")).isTrue();
    }

    @Test
    void notFoundUrl_isUnavailable() {
        assertThat(availability.isAvailable(baseUrl + "/gone")).isFalse();
    }

    @Test
    void serverErrorUrl_staysAvailable_failOpen() {
        assertThat(availability.isAvailable(baseUrl + "/broken")).isTrue();
    }

    @Test
    void headNotAllowed_fallsBackToGet() {
        assertThat(availability.isAvailable(baseUrl + "/no-head")).isTrue();
        assertThat(availability.isAvailable(baseUrl + "/no-head-gone")).isFalse();
    }

    @Test
    void unreachableHost_staysAvailable_failOpen() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        assertThat(availability.isAvailable("http://127.0.0.1:" + closedPort + "/x")).isTrue();
    }

    @Test
    void blankOrNullUrl_isUnavailable() {
        assertThat(availability.isAvailable(null)).isFalse();
        assertThat(availability.isAvailable("  ")).isFalse();
    }

    private static void send(HttpExchange exchange, int status) throws IOException {
        byte[] body = "x".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
