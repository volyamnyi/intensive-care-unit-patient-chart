package com.superhumans.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private StompEndpointRegistry stompEndpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration registration;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigins",
                new String[]{"http://localhost:5173", "http://localhost:3000"});
    }

    @Test
    void configureMessageBroker_shouldSetBrokerAndPrefixes() {
        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        verify(messageBrokerRegistry).enableSimpleBroker("/topic", "/queue");
        verify(messageBrokerRegistry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void registerStompEndpoints_shouldConfigureWebSocketEndpoint() {
        when(stompEndpointRegistry.addEndpoint("/ws")).thenReturn(registration);
        when(registration.setAllowedOrigins("http://localhost:5173", "http://localhost:3000"))
                .thenReturn(registration);

        webSocketConfig.registerStompEndpoints(stompEndpointRegistry);

        verify(stompEndpointRegistry).addEndpoint("/ws");
        verify(registration).setAllowedOrigins("http://localhost:5173", "http://localhost:3000");
        verify(registration).withSockJS();
    }

    @Test
    void registerStompEndpoints_shouldUseConfiguredAllowedOrigins() {
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigins",
                new String[]{"https://hospital.ua"});
        when(stompEndpointRegistry.addEndpoint("/ws")).thenReturn(registration);
        when(registration.setAllowedOrigins("https://hospital.ua")).thenReturn(registration);

        webSocketConfig.registerStompEndpoints(stompEndpointRegistry);

        verify(registration).setAllowedOrigins("https://hospital.ua");
        verify(registration).withSockJS();
    }

    @Test
    void registerStompEndpoints_withEmptyOrigins_passesEmptyArray() {
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigins", new String[]{});
        when(stompEndpointRegistry.addEndpoint("/ws")).thenReturn(registration);
        when(registration.setAllowedOrigins()).thenReturn(registration);

        webSocketConfig.registerStompEndpoints(stompEndpointRegistry);

        verify(registration).setAllowedOrigins();
        verify(registration).withSockJS();
    }
}
