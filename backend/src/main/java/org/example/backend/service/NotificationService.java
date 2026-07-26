package org.example.backend.service;

import org.example.backend.dto.NotificationDTO;
import org.example.backend.entity.Notification;
import org.example.backend.entity.User;
import org.example.backend.repository.NotificationRepository;
import org.example.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Notification service với retry + log chuẩn SLF4J.
 *
 * <p><b>Quick Win F1.7:</b> Trước đó mọi lỗi broadcast đều bị nuốt
 * (chỉ print StackTrace ra System.err), user không bao giờ biết notification
 * thất bại. Giờ wrap trong {@link #broadcastWithRetry} với exponential
 * backoff, log đầy đủ ở 3 cấp:
 * <ul>
 *   <li>INFO — gửi thành công</li>
 *   <li>WARN — retry (mất kết nối WebSocket nhưng DB vẫn OK)</li>
 *   <li>ERROR — cả retry cũng fail, cần manual check</li>
 * </ul>
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final int MAX_BROADCAST_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 200;

    private final NotificationRepository notifRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepo;

    public NotificationService(NotificationRepository notifRepo, SimpMessagingTemplate messagingTemplate, UserRepository userRepo) {
        this.notifRepo = notifRepo;
        this.messagingTemplate = messagingTemplate;
        this.userRepo = userRepo;
    }

    /** Create and broadcast a notification (idempotent nhờ DB lưu trước). */
    public NotificationDTO createAndSend(User user, String title, String message, String type, UUID taskId) {
        if (user == null) {
            log.warn("createAndSend called with null user (title={}, type={})", title, type);
            return null;
        }
        Notification saved = persistNotification(user, title, message, type, taskId);
        NotificationDTO dto = toDTO(saved);
        broadcastWithRetry(user.getId(), dto, title);
        return dto;
    }

    /** Overload accepting userId as UUID */
    public NotificationDTO createAndSend(UUID userId, String title, String message, String type, UUID taskId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            log.warn("createAndSend called with missing userId={} (title={})", userId, title);
            return null;
        }
        return createAndSend(user, title, message, type, taskId);
    }

    /**
     * Lưu notification vào DB — luôn chạy trong transaction riêng để retry
     * broadcast KHÔNG rollback việc lưu DB. Nếu DB fail, throw ngay để caller
     * biết.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Notification persistNotification(User user, String title, String message, String type, UUID taskId) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setTaskId(taskId);
        n.setIsRead(false);
        Notification saved = notifRepo.save(n);
        log.info("Notification persisted: id={}, user={}, type={}, title={}",
                saved.getId(), user.getId(), type, title);
        return saved;
    }

    /**
     * Broadcast qua WebSocket với exponential backoff retry.
     * Fallback an toàn: nếu retry fail, log ERROR nhưng KHÔNG throw — caller
     * không nên bị ảnh hưởng vì notification đã lưu DB rồi, user F5 vẫn thấy.
     */
    private void broadcastWithRetry(UUID userId, NotificationDTO dto, String title) {
        String destination = "/topic/user/" + userId + "/notifications";
        long backoff = INITIAL_BACKOFF_MS;
        for (int attempt = 1; attempt <= MAX_BROADCAST_RETRIES; attempt++) {
            try {
                messagingTemplate.convertAndSend(destination, dto);
                if (attempt == 1) {
                    log.info("Notification broadcast: user={}, type={}, title={}",
                            userId, dto.getType(), title);
                } else {
                    log.info("Notification broadcast succeeded on retry {}/{}: user={}, title={}",
                            attempt, MAX_BROADCAST_RETRIES, userId, title);
                }
                return;
            } catch (RuntimeException ex) {
                if (attempt == MAX_BROADCAST_RETRIES) {
                    log.error("Notification broadcast FAILED after {} attempts: user={}, title={}, error={}",
                            MAX_BROADCAST_RETRIES, userId, title, ex.getMessage(), ex);
                    return;
                }
                log.warn("Notification broadcast attempt {}/{} failed: user={}, title={}, error={}. Retrying in {}ms",
                        attempt, MAX_BROADCAST_RETRIES, userId, title, ex.getMessage(), backoff);
                sleep(backoff);
                backoff *= 2;
            }
        }
    }

    private static void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getByUser(UUID userId) {
        return notifRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notifRepo.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notifRepo.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notifRepo.save(n);
            log.debug("Notification marked read: id={}", notificationId);
        });
    }

    /**
     * Verifies that a notification belongs to a given user. Prevents IDOR on
     * mark-as-read by ensuring the caller owns the target notification.
     */
    @Transactional(readOnly = true)
    public boolean canUserModifyNotification(UUID notificationId, UUID userId) {
        if (notificationId == null || userId == null) {
            return false;
        }
        return notifRepo.findById(notificationId)
                .map(n -> n.getUser() != null && userId.equals(n.getUser().getId()))
                .orElse(false);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        List<Notification> unread = notifRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().filter(n -> !n.getIsRead()).collect(Collectors.toList());
        unread.forEach(n -> n.setIsRead(true));
        notifRepo.saveAll(unread);
        log.info("Marked {} notifications as read for user {}", unread.size(), userId);
    }

    private NotificationDTO toDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId().toString());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setType(n.getType());
        dto.setTaskId(n.getTaskId() != null ? n.getTaskId().toString() : null);
        dto.setRead(n.getIsRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
