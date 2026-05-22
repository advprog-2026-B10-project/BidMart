package id.ac.ui.cs.advprog.bidmart.bidding.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BiddingEventListener {

    @Autowired
    private RabbitTemplate  rabbitTemplate;

    @EventListener
    @Async
    public void handleBidPlaced(BidPlacedEvent event) {
        System.out.println("[EVENT] BidPlaced - auctionId: " + event.getAuctionId()
            + ", buyerId: " + event.getBuyerId()
            + ", amount: " + event.getAmount());

        Map<String, Object> message = new HashMap<>();
        message.put("catalogId", event.getAuctionId());
        message.put("newPrice", event.getAmount());

        rabbitTemplate.convertAndSend("bid_exchange", "bid_routingKey", message);
        System.out.println(">>> Berhasil nembak RabbitMQ untuk update harga Katalog!");
    }

    @EventListener
    @Async
    public void handleAuctionEnded(AuctionEndedEvent event) {
        System.out.println("[EVENT] AuctionEnded - auctionId: " + event.getAuctionId()
                + ", status: " + event.getFinalStatus()
                + ", winnerId: " + event.getWinnerId()
                + ", amount: " + event.getFinalAmount());
    }
}
