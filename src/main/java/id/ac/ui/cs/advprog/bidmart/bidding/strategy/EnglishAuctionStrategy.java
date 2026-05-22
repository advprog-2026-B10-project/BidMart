package id.ac.ui.cs.advprog.bidmart.bidding.strategy;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.*;
import id.ac.ui.cs.advprog.bidmart.bidding.event.*;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.*;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

@Component
public class EnglishAuctionStrategy implements AuctionStrategy {

    @Autowired
    private WalletService walletService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public String placeBid(Auction auction, String userId, Double amount) {
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            return "Auction sudah berakhir";
        }

        Double highestBid = auction.getStartingPrice();
        Bid previousHighest = null;

        if (!auction.getBids().isEmpty()) {
            for (Bid bid : auction.getBids()) {
                if (bid.getAmount() > highestBid) {
                    highestBid = bid.getAmount();
                    previousHighest = bid;
                }
            }
        }

        if (amount <= highestBid) {
            return "Bid harus lebih tinggi dari penawaran tertinggi saat ini";
        }

        if (previousHighest != null) {
            walletService.releaseHeldBalance(previousHighest.getBuyerId(), previousHighest.getAmount());
        }
        walletService.holdBalance(userId, amount);

        Bid bid = new Bid();
        bid.setAmount(amount);
        bid.setAuction(auction);
        bid.setTimestamp(LocalDateTime.now());
        bid.setBuyerId(userId);
        bidRepository.save(bid);

        LocalDateTime twoMinutesFromNow = LocalDateTime.now().plusMinutes(2);
        if (twoMinutesFromNow.isAfter(auction.getEndTime())) {
            auction.setEndTime(LocalDateTime.now().plusMinutes(2));
            auction.setStatus(AuctionStatus.EXTENDED);
        }

        auctionRepository.save(auction);
        eventPublisher.publishEvent(new BidPlacedEvent(this, auction.getId(), userId, amount));
        return "Bid berhasil, saldo ditahan";
    }

    @Override
    public String determineWinner(Auction auction) {
        Optional<Bid> highestBid = auction.getBids().stream()
                .max(Comparator.comparingDouble(Bid::getAmount));

        if (highestBid.isEmpty()) {
            auction.setStatus(AuctionStatus.UNSOLD);
            auctionRepository.save(auction);
            eventPublisher.publishEvent(new AuctionEndedEvent(this, auction.getId(), null, 0.0, AuctionStatus.UNSOLD));
            return "Lelang berakhir tanpa pemenang";
        }

        if (highestBid.get().getAmount() >= auction.getReservePrice()) {
            auction.setStatus(AuctionStatus.WON);
            walletService.deductHeldBalance(highestBid.get().getBuyerId(), highestBid.get().getAmount());
            eventPublisher.publishEvent(new AuctionEndedEvent(this, auction.getId(),
                    highestBid.get().getBuyerId(), highestBid.get().getAmount(), AuctionStatus.WON));
        } else {
            auction.setStatus(AuctionStatus.UNSOLD);
            walletService.releaseHeldBalance(highestBid.get().getBuyerId(), highestBid.get().getAmount());
            eventPublisher.publishEvent(new AuctionEndedEvent(this, auction.getId(),
                    null, highestBid.get().getAmount(), AuctionStatus.UNSOLD));
        }

        auctionRepository.save(auction);
        return "User menang lelang, saldo dipotong";
    }
}