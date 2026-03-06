package id.ac.ui.cs.advprog.bidmart.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "type", nullable = false)
    private String type; // AUCTION_WON, NEW_BID, ORDER_UPDATE, SYSTEM
    
    @Column(name = "channel", nullable = false)
    private String channel; // EMAIL, PUSH, IN_APP
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead;
    
    @Column(name = "reference_id")
    private String referenceId; // Reference to related entity (auction_id, order_id, etc.)
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
    }
}
