package id.ac.ui.cs.advprog.bidmart.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private Boolean inAppEnabled = true;

    @Column(name = "auction_status_enabled", nullable = false)
    @Builder.Default
    private Boolean auctionStatusEnabled = true;

    @Column(name = "order_update_enabled", nullable = false)
    @Builder.Default
    private Boolean orderUpdateEnabled = true;

    @Column(name = "transaction_enabled", nullable = false)
    @Builder.Default
    private Boolean transactionEnabled = true;

    @Column(name = "system_enabled", nullable = false)
    @Builder.Default
    private Boolean systemEnabled = true;
}
