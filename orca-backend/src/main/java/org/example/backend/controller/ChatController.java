package org.example.backend.controller;

import org.example.backend.dto.ChatMessageDTO;
import org.example.backend.entity.User;
import org.example.backend.service.ChatService;
import org.example.backend.service.NotificationService;
import org.example.backend.service.PresenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;
    private final NotificationService notificationService;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate,
                          PresenceService presenceService, NotificationService notificationService) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
        this.notificationService = notificationService;
    }

    /** Get group chat messages */
    @GetMapping("/teams/{teamId}/chat")
    public ResponseEntity<List<ChatMessageDTO>> getGroupMessages(@PathVariable UUID teamId) {
        return ResponseEntity.ok(chatService.getGroupMessages(teamId));
    }

    /** Get DM with a specific user */
    @GetMapping("/teams/{teamId}/chat/dm/{userId}")
    public ResponseEntity<List<ChatMessageDTO>> getDirectMessages(
            @PathVariable UUID teamId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getDirectMessages(teamId, currentUser.getId(), userId));
    }

    /** Get last DM message for each contact */
    @GetMapping("/teams/{teamId}/chat/dm-previews")
    public ResponseEntity<List<ChatMessageDTO>> getDmPreviews(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(chatService.getLastDmMessages(teamId, currentUser.getId()));
    }

    /** Send a message (group or DM) — also broadcasts via WebSocket */
    @PostMapping("/teams/{teamId}/chat")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable UUID teamId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String content = body.get("content");
        String recipientIdStr = body.get("recipientId");

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        UUID recipientId = null;
        if (recipientIdStr != null && !recipientIdStr.isBlank()) {
            recipientId = UUID.fromString(recipientIdStr);
        }

        ChatMessageDTO saved = chatService.sendMessage(teamId, currentUser, recipientId, content);

        // Broadcast via WebSocket
        if (recipientId == null) {
            // Group message → broadcast to /topic/team/{teamId}
            messagingTemplate.convertAndSend("/topic/team/" + teamId, saved);
        } else {
            // DM → broadcast to both sender and recipient private channels
            messagingTemplate.convertAndSend("/topic/dm/" + teamId + "/" + currentUser.getId() + "/" + recipientId, saved);
            messagingTemplate.convertAndSend("/topic/dm/" + teamId + "/" + recipientId + "/" + currentUser.getId(), saved);

            // Send DM notification to recipient
            String senderName = currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername();
            String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            notificationService.createAndSend(
                    recipientId,
                    "Tin nhắn mới từ " + senderName,
                    preview,
                    "CHAT_MESSAGE",
                    null
            );
        }

        return ResponseEntity.ok(saved);
    }

    /** Get online users */
    @GetMapping("/presence/online")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUserIds());
    }
}
