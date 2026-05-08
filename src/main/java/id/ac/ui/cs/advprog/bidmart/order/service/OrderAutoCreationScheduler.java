package id.ac.ui.cs.advprog.bidmart.order.service;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoCreationScheduler {

    private final AuctionRepository auctionRepository;
    private final OrderService orderService;

    @Scheduled(fixedRate = 30_000)
    public void tick() {
        List<Auction> wonAuctions = auctionRepository.findByStatusIn(List.of(AuctionStatus.WON));
        for (Auction a : wonAuctions) {
            try {
                orderService.createFromWonAuction(a);
            } catch (Exception e) {
                log.error("Failed to create order for auction {}: {}", a.getId(), e.getMessage());
            }
        }
    }
}
