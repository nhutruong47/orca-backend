package org.example.backend.config;

import org.example.backend.service.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
public class WebSocketEventListener {

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(PresenceService presenceService, SimpMessagingTemplate messagingTemplate) {
        this.presenceService = presenceService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // Get userId from native headers (client sends it as a STOMP header)
        var nativeHeaders = accessor.toNativeHeaderMap();
        if (nativeHeaders.containsKey("userId")) {
            String userId = nativeHeaders.get("userId").get(0);
            if (userId != null && !userId.isBlank()) {
                presenceService.userConnected(userId, sessionId);

                // Store userId in session attributes for disconnect handling
                Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                if (sessionAttrs != null) {
                    sessionAttrs.put("userId", userId);
                }

                broadcastOnlineUsers();
            }
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        presenceService.userDisconnected(sessionId);
        broadcastOnlineUsers();
    }

    private void broadcastOnlineUsers() {
        messagingTemplate.convertAndSend("/topic/presence", presenceService.getOnlineUserIds());
    }
}
