package id.ac.ui.cs.advprog.bidmart.notification.listener;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Bid;
import id.ac.ui.cs.advprog.bidmart.bidding.event.AuctionEndedEvent;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.service.NotificationService;
import id.ac.ui.cs.advprog.bidmart.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionOutcomeListener {

    private final AuctionRepository auctionRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;

    @EventListener
    @Async
    public void onAuctionEnded(AuctionEndedEvent event) {
        try {
            Optional<Auction> auctionOpt = auctionRepository.findById(event.getAuctionId());
            if (auctionOpt.isEmpty()) {
                log.warn("Auction not found for AuctionEndedEvent: {}", event.getAuctionId());
                return;
            }
            Auction auction = auctionOpt.get();

            if (event.getFinalStatus() == AuctionStatus.WON) {
                handleWon(auction);
            } else if (event.getFinalStatus() == AuctionStatus.UNSOLD) {
                handleUnsold(auction);
            }
        } catch (Exception e) {
            log.error("Failed to handle AuctionEndedEvent for auction {}: {}",
                    event.getAuctionId(), e.getMessage());
        }
    }

    private void handleWon(Auction auction) {
        List<Bid> bids = auction.getBids();
        if (bids == null || bids.isEmpty()) {
            log.warn("WON auction {} has no bids; skipping notifications", auction.getId());
            return;
        }
        orderService.createFromWonAuction(auction);

        Bid winner = bids.stream()
                .max((a, b) -> Double.compare(a.getAmount(), b.getAmount()))
                .orElseThrow();
        String ref = String.valueOf(auction.getId());

        notificationService.send(
                winner.getBuyerId(),
                NotificationType.AUCTION_WON,
                "Kamu menang!",
                "Selamat, kamu memenangkan lelang #" + auction.getId()
                        + " dengan bid Rp " + winner.getAmount(),
                ref);

        Set<String> losers = bids.stream()
                .map(Bid::getBuyerId)
                .filter(id -> !id.equals(winner.getBuyerId()))
                .collect(Collectors.toSet());
        for (String userId : losers) {
            notificationService.send(
                    userId,
                    NotificationType.AUCTION_LOST,
                    "Lelang selesai",
                    "Kamu tidak memenangkan lelang #" + auction.getId(),
                    ref);
        }
    }

    private void handleUnsold(Auction auction) {
        List<Bid> bids = auction.getBids();
        if (bids == null || bids.isEmpty()) return;
        String ref = String.valueOf(auction.getId());
        Set<String> bidders = bids.stream().map(Bid::getBuyerId).collect(Collectors.toSet());
        for (String userId : bidders) {
            notificationService.send(
                    userId,
                    NotificationType.AUCTION_UNSOLD,
                    "Lelang berakhir tanpa pemenang",
                    "Lelang #" + auction.getId() + " berakhir tanpa pemenang (harga cadangan tidak tercapai)",
                    ref);
        }
    }
}
