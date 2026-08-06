package org.example.backend.controller;

import org.example.backend.dto.ChatMessageDTO;
import org.example.backend.entity.User;
import org.example.backend.service.AccessControlService;
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
    private final AccessControlService accessControlService;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate,
                          PresenceService presenceService, NotificationService notificationService,
                          AccessControlService accessControlService) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
        this.notificationService = notificationService;
        this.accessControlService = accessControlService;
    }

    @GetMapping("/teams/{teamId}/chat")
    public ResponseEntity<?> getGroupMessages(@PathVariable UUID teamId,
                                              @AuthenticationPrincipal User currentUser) {
        try {
            accessControlService.validateTeamAccess(currentUser.getId(), teamId);
            return ResponseEntity.ok(chatService.getGroupMessages(teamId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/{teamId}/chat/dm/{userId}")
    public ResponseEntity<?> getDirectMessages(
            @PathVariable UUID teamId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal User currentUser) {
        try {
            accessControlService.validateTeamAccess(currentUser.getId(), teamId);
            return ResponseEntity.ok(chatService.getDirectMessages(teamId, currentUser.getId(), userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/teams/{teamId}/chat/dm-previews")
    public ResponseEntity<?> getDmPreviews(
            @PathVariable UUID teamId,
            @AuthenticationPrincipal User currentUser) {
        try {
            accessControlService.validateTeamAccess(currentUser.getId(), teamId);
            return ResponseEntity.ok(chatService.getLastDmMessages(teamId, currentUser.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/teams/{teamId}/chat")
    public ResponseEntity<?> sendMessage(
            @PathVariable UUID teamId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        accessControlService.validateTeamAccess(currentUser.getId(), teamId);
        String content = body.get("content");
        String recipientIdStr = body.get("recipientId");
        String attachmentUrl = body.get("attachmentUrl");
        String attachmentName = body.get("attachmentName");
        String attachmentType = body.get("attachmentType");

        if ((content == null || content.isBlank()) && (attachmentUrl == null || attachmentUrl.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tin nhắn không được để trống"));
        }

        UUID recipientId = null;
        if (recipientIdStr != null && !recipientIdStr.isBlank()) {
            recipientId = UUID.fromString(recipientIdStr);
        }

        ChatMessageDTO saved = chatService.sendMessage(teamId, currentUser, recipientId, content, attachmentUrl, attachmentName, attachmentType);

        if (recipientId == null) {
            messagingTemplate.convertAndSend("/topic/team/" + teamId, saved);
        } else {
            messagingTemplate.convertAndSend("/topic/dm/" + teamId + "/" + currentUser.getId() + "/" + recipientId, saved);
            messagingTemplate.convertAndSend("/topic/dm/" + teamId + "/" + recipientId + "/" + currentUser.getId(), saved);

            String senderName = currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername();
            String preview = (content != null && content.length() > 50) ? content.substring(0, 50) + "..." : (content != null && !content.isBlank() ? content : "Đã gửi một đính kèm");
            notificationService.createAndSend(
                    recipientId,
                    "Tin nhắn mới từ " + senderName,
                    preview,
                    "CHAT_MESSAGE",
                    teamId
            );
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/presence/online")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUserIds());
    }
}
