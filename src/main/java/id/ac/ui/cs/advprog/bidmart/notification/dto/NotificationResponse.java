package id.ac.ui.cs.advprog.bidmart.notification.dto;

import id.ac.ui.cs.advprog.bidmart.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private String channel;
    private Boolean isRead;
    private String referenceId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getTitle(), n.getMessage(),
                n.getType().name(), n.getChannel().name(),
                n.getIsRead(), n.getReferenceId(),
                n.getCreatedAt(), n.getReadAt());
    }
}
