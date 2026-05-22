package id.ac.ui.cs.advprog.bidmart.bidding.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class BidPlacedEvent extends ApplicationEvent {
    private final Long auctionId;
    private final String buyerId;
    private final Double amount;

    public BidPlacedEvent(Object source, Long auctionId, String buyerId, Double amount) {
        super(source);
        this.auctionId = auctionId;
        this.buyerId = buyerId;
        this.amount = amount;
    }
    
    
}
