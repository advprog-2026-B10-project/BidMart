package id.ac.ui.cs.advprog.bidmart.notification.controller;

import id.ac.ui.cs.advprog.bidmart.notification.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmart.notification.entity.Notification;
import id.ac.ui.cs.advprog.bidmart.notification.repository.NotificationRepository;
import id.ac.ui.cs.advprog.bidmart.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @GetMapping("/me")
    public List<NotificationResponse> getMine(Authentication authentication) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(authentication.getName())
                .stream().map(NotificationResponse::from).toList();
    }

    @GetMapping("/me/unread-count")
    public Map<String, Long> unreadCount(Authentication authentication) {
        long count = notificationRepository.countByUserIdAndIsRead(authentication.getName(), false);
        return Map.of("count", count);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable Long id,
            Authentication authentication) {
        Notification n = notificationService.markAsRead(id, authentication.getName());
        return ResponseEntity.ok(NotificationResponse.from(n));
    }

    @GetMapping("/{userId}")
    public List<NotificationResponse> getUserNotifications(
            @PathVariable String userId,
            Authentication authentication) {
        if (!authentication.getName().equals(userId)) {
            throw new SecurityException("Cannot view another user's notifications");
        }
        return notificationRepository.findByUserId(userId).stream()
                .map(NotificationResponse::from).toList();
    }
}
