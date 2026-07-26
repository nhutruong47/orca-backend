package org.example.backend.service;

import org.example.backend.dto.NotificationDTO;
import org.example.backend.entity.Notification;
import org.example.backend.entity.User;
import org.example.backend.repository.NotificationRepository;
import org.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test cho NotificationService — focus vào retry logic và idempotent persist.
 *
 * <p>Đảm bảo:
 * <ul>
 *   <li>Retry tối đa 3 lần khi broadcast fail</li>
 *   <li>Không throw exception ra caller (DB đã lưu, F5 vẫn thấy)</li>
 *   <li>User null → log warning, return null, không crash</li>
 *   <li>UserId UUID không tồn tại → log warning, không crash</li>
 * </ul>
 */
class NotificationRetryTest {

    private NotificationRepository notifRepo;
    private SimpMessagingTemplate messagingTemplate;
    private UserRepository userRepo;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notifRepo = mock(NotificationRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        userRepo = mock(UserRepository.class);
        service = new NotificationService(notifRepo, messagingTemplate, userRepo);
    }

    private User user() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("alice");
        return u;
    }

    private Notification persisted(User u) {
        Notification n = new Notification();
        n.setId(UUID.randomUUID());
        n.setUser(u);
        n.setTitle("Test");
        n.setMessage("body");
        n.setType("TEST");
        n.setIsRead(false);
        return n;
    }

    @Test
    @DisplayName("happy path: persist + broadcast ONCE")
    void happyPath() {
        User u = user();
        when(notifRepo.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        NotificationDTO dto = service.createAndSend(u, "Hi", "Body", "ORDER_CREATED", null);

        verify(notifRepo).save(any(Notification.class));
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/user/" + u.getId() + "/notifications"), any(Object.class));
        assertThat(dto).isNotNull();
        assertThat(dto.getTitle()).isEqualTo("Hi");
    }

    @Test
    @DisplayName("broadcast fail 1 lần → retry lần 2 thành công")
    void retryOnce() {
        User u = user();
        when(notifRepo.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        doThrow(new RuntimeException("WS down"))
                .doNothing()
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        NotificationDTO dto = service.createAndSend(u, "Hi", "Body", "ORDER_CREATED", null);

        assertThat(dto).isNotNull();
        verify(messagingTemplate, times(2))
                .convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("broadcast fail 3 lần liên tiếp → no throw, vẫn lưu DB")
    void retryExhausted_doesNotThrow() {
        User u = user();
        when(notifRepo.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        doThrow(new RuntimeException("WS permanently down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        NotificationDTO dto = service.createAndSend(u, "Hi", "Body", "ORDER_CREATED", null);

        // Caller KHÔNG bao giờ nhận exception — DB đã lưu, F5 vẫn thấy
        assertThat(dto).isNotNull();
        verify(notifRepo).save(any(Notification.class));
        verify(messagingTemplate, times(3))
                .convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("user null → return null, không crash, không save")
    void nullUser() {
        NotificationDTO dto = service.createAndSend((User) null, "Hi", "Body", "ORDER_CREATED", null);
        assertThat(dto).isNull();
        verifyNoInteractions(notifRepo, messagingTemplate);
    }

    @Test
    @DisplayName("userId UUID không tồn tại → return null, không crash")
    void missingUserId() {
        UUID missingId = UUID.randomUUID();
        when(userRepo.findById(missingId)).thenReturn(Optional.empty());

        NotificationDTO dto = service.createAndSend(missingId, "Hi", "Body", "ORDER_CREATED", null);

        assertThat(dto).isNull();
        verify(notifRepo, never()).save(any(Notification.class));
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("persist fail (DB exception) → throw để caller biết")
    void persistFailure_propagates() {
        User u = user();
        when(notifRepo.save(any(Notification.class)))
                .thenThrow(new RuntimeException("DB down"));

        try {
            service.createAndSend(u, "Hi", "Body", "ORDER_CREATED", null);
            // If we reach here, test must fail
            assertThat(false).as("Expected exception").isTrue();
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage()).contains("DB down");
        }
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("markAllRead đánh dấu đúng số notification")
    void markAllRead() {
        UUID userId = UUID.randomUUID();
        User u = user();
        u.setId(userId);
        Notification n1 = persisted(u);
        Notification n2 = persisted(u);
        n2.setIsRead(false);
        n1.setIsRead(false);
        when(notifRepo.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(java.util.List.of(n1, n2));

        service.markAllRead(userId);

        assertThat(n1.getIsRead()).isTrue();
        assertThat(n2.getIsRead()).isTrue();
        verify(notifRepo).saveAll(any());
    }

    @Test
    @DisplayName("canUserModifyNotification IDOR guard")
    void canUserModifyNotification() {
        UUID notificationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        User owner = user();
        owner.setId(ownerId);
        Notification n = persisted(owner);
        when(notifRepo.findById(notificationId)).thenReturn(Optional.of(n));

        assertThat(service.canUserModifyNotification(notificationId, ownerId)).isTrue();
        assertThat(service.canUserModifyNotification(notificationId, otherId)).isFalse();
        assertThat(service.canUserModifyNotification(notificationId, null)).isFalse();
        assertThat(service.canUserModifyNotification(null, ownerId)).isFalse();
    }
}
