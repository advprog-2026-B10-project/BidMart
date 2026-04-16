package id.ac.ui.cs.advprog.bidmart.notification.scheduler;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auction_notification_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionNotificationState {

    @Id
    @Column(name = "auction_id")
    private Long auctionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_notified_status", nullable = false)
    private AuctionStatus lastNotifiedStatus;
}
