package id.ac.ui.cs.advprog.bidmart.bidding.service;

import id.ac.ui.cs.advprog.bidmart.bidding.dto.CreateAuctionRequest;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionType;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.strategy.AuctionStrategy;
import id.ac.ui.cs.advprog.bidmart.bidding.strategy.AuctionStrategyFactory;
import jakarta.persistence.OptimisticLockException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BiddingService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionStrategyFactory strategyFactory;

    public Auction createAuction(CreateAuctionRequest request) {

        Auction auction = new Auction();

        auction.setListingId(request.getListingId());
        auction.setStartingPrice(request.getStartingPrice());
        auction.setReservePrice(request.getReservePrice());

        LocalDateTime endTime = LocalDateTime.now().plusMinutes(request.getDurationInMinutes());
        auction.setEndTime(endTime);

        auction.setStatus(AuctionStatus.DRAFT);

        auction.setSellerId(request.getSellerId());

        auction.setType(request.getType() != null ? request.getType() : AuctionType.ENGLISH);
        return auctionRepository.save(auction);
    }

    public String placeBid(String userId, Long auctionId, Double amount) {
        try {
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new RuntimeException("Auction tidak ditemukan"));

            if (!auction.getStatus().equals(AuctionStatus.ACTIVE) &&
                    !auction.getStatus().equals(AuctionStatus.EXTENDED)) {
                return "Auction tidak dalam status aktif";
            }

            AuctionStrategy strategy = strategyFactory.getStrategy(auction.getType());
            return strategy.placeBid(auction, userId, amount);

        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            return "Sistem sedang sibuk, silakan coba lagi";
        }
    }

    public String determineWinner(Auction auction) {
        AuctionStrategy strategy = strategyFactory.getStrategy(auction.getType());
        return strategy.determineWinner(auction);
    }

    @Scheduled(fixedRate = 60000)
    public void closeExpiredAuctions() {
        List<Auction> activeAuctions = auctionRepository.findByStatusIn(List.of(AuctionStatus.ACTIVE, AuctionStatus.EXTENDED));

        for (Auction auction: activeAuctions) {
            if (LocalDateTime.now().isAfter(auction.getEndTime())) {
                auction.setStatus(AuctionStatus.CLOSED);
                determineWinner(auction);
            }
        }
    }
}