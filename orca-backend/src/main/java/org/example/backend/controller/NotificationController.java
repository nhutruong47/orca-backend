package org.example.backend.controller;

import org.example.backend.dto.NotificationDTO;
import org.example.backend.entity.User;
import org.example.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getByUser(user.getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(user.getId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Đã đọc"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "Đã đọc tất cả"));
    }
}
