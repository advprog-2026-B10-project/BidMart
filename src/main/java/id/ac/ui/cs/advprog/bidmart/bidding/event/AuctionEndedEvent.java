package id.ac.ui.cs.advprog.bidmart.bidding.event;

import org.springframework.context.ApplicationEvent;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import lombok.Getter;

@Getter
public class AuctionEndedEvent extends ApplicationEvent {
    private final Long auctionId;
    private final String winnerId; // null if unsold
    private final Double finalAmount;
    private final AuctionStatus finalStatus;

    public AuctionEndedEvent(Object source, Long auctionId, String winnerId, Double finalAmount, AuctionStatus finalStatus) {
        super(source);
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.finalAmount = finalAmount;
        this.finalStatus = finalStatus;
    }
}
