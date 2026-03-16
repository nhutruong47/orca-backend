package org.example.backend.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    // userId -> set of session IDs
    private final Map<String, Set<String>> onlineUsers = new ConcurrentHashMap<>();

    public void userConnected(String userId, String sessionId) {
        onlineUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public void userDisconnected(String sessionId) {
        onlineUsers.forEach((userId, sessions) -> {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                onlineUsers.remove(userId);
            }
        });
    }

    public Set<String> getOnlineUserIds() {
        return Collections.unmodifiableSet(onlineUsers.keySet());
    }

    public boolean isOnline(String userId) {
        Set<String> sessions = onlineUsers.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
}
