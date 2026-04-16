package id.ac.ui.cs.advprog.bidmart.notification.service;

import id.ac.ui.cs.advprog.bidmart.notification.entity.Notification;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationChannel;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationPreference;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService preferenceService;

    public void send(String userId,
                     NotificationType type,
                     String title,
                     String message,
                     String referenceId) {
        NotificationPreference pref = preferenceService.getOrDefault(userId);
        if (!pref.getInAppEnabled()) return;
        if (!allowedByCategory(pref, type)) return;

        Notification n = Notification.builder()
                .userId(userId)
                .type(type)
                .channel(NotificationChannel.IN_APP)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        notificationRepository.save(n);
    }

    public Notification markAsRead(Long id, String userId) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!n.getUserId().equals(userId)) {
            throw new SecurityException("Cannot mark another user's notification");
        }
        n.setIsRead(true);
        n.setReadAt(LocalDateTime.now());
        return notificationRepository.save(n);
    }

    private boolean allowedByCategory(NotificationPreference pref, NotificationType type) {
        return switch (type) {
            case AUCTION_WON, AUCTION_LOST, AUCTION_UNSOLD, NEW_BID -> pref.getAuctionStatusEnabled();
            case ORDER_CREATED, ORDER_UPDATE -> pref.getOrderUpdateEnabled();
            case TRANSACTION -> pref.getTransactionEnabled();
            case SYSTEM -> pref.getSystemEnabled();
        };
    }
}
