package org.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WebSocket / STOMP configuration.
 *
 * <p>Origins are restricted to an explicit allowlist loaded from
 * {@code app.websocket.allowed-origins} (comma-separated). Defaults to
 * {@code http://localhost:5173,http://localhost:3000,http://localhost:8080}
 * so local dev works out of the box. For staging/production, set the property
 * to your real frontend hosts (e.g. {@code https://app.orca.vn}).
 *
 * <p><b>Security:</b> the previous {@code setAllowedOriginPatterns("*")}
 * allowed any website to open a SockJS connection to this backend, exposing
 * the broker to Cross-Site WebSocket Hijacking.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final List<String> allowedOrigins;

    public WebSocketConfig(@Value("${app.websocket.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8080}") String allowedOriginsCsv) {
        this.allowedOrigins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                .withSockJS();
    }
}