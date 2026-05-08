package id.ac.ui.cs.advprog.bidmart.bidding.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class BiddingEventListener {
    
    @EventListener
    @Async
    public void handleBidPlaced(BidPlacedEvent event) {
        System.out.println("[EVENT] BidPlaced - auctionId: " + event.getAuctionId()
            + ", buyerId: " + event.getBuyerId() 
            + ", amount: " + event.getAmount());
            // bisa dihubungkan ke modul notifikasi
    }

    @EventListener
    @Async
    public void handleAuctionEnded(AuctionEndedEvent event) {
        System.out.println("[EVENT] AuctionEnded - auctionId: " + event.getAuctionId()
            + ", status: " + event.getFinalStatus()
            + ", winnerId: " + event.getWinnerId()
            + ", amount: " + event.getFinalAmount());
            // bisa dihubungkan ke modul notifikasi
    }
}
