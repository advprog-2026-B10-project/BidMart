package id.ac.ui.cs.advprog.bidmart.notification.scheduler;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Bid;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.notification.entity.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionOutcomeNotifier {

    private static final List<AuctionStatus> TERMINAL = List.of(AuctionStatus.WON, AuctionStatus.UNSOLD);

    private final AuctionRepository auctionRepository;
    private final AuctionNotificationStateRepository stateRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 30_000)
    public void tick() {
        List<Auction> terminal = auctionRepository.findByStatusIn(TERMINAL);
        for (Auction a : terminal) {
            try {
                processOne(a);
            } catch (Exception e) {
                log.error("Failed to notify outcome for auction {}: {}", a.getId(), e.getMessage());
            }
        }
    }

    private void processOne(Auction auction) {
        Optional<AuctionNotificationState> existing = stateRepository.findById(auction.getId());
        if (existing.isPresent() && existing.get().getLastNotifiedStatus() == auction.getStatus()) {
            return;
        }

        if (auction.getStatus() == AuctionStatus.WON) {
            notifyWon(auction);
        } else if (auction.getStatus() == AuctionStatus.UNSOLD) {
            notifyUnsold(auction);
        }

        stateRepository.save(AuctionNotificationState.builder()
                .auctionId(auction.getId())
                .lastNotifiedStatus(auction.getStatus())
                .build());
    }

    private void notifyWon(Auction auction) {
        List<Bid> bids = auction.getBids();
        if (bids == null || bids.isEmpty()) return;
        Bid winner = bids.stream().max(Comparator.comparingDouble(Bid::getAmount)).get();
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

    private void notifyUnsold(Auction auction) {
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
