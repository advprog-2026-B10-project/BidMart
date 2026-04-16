package id.ac.ui.cs.advprog.bidmart.bidding.service;

import id.ac.ui.cs.advprog.bidmart.bidding.dto.CreateAuctionRequest;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Bid;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.BidRepository;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BiddingService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private WalletService walletService;

    public Auction createAuction(CreateAuctionRequest request) {

        Auction auction = new Auction();

        auction.setListingId(request.getListingId());
        auction.setStartingPrice(request.getStartingPrice());
        auction.setReservePrice(request.getReservePrice());

        LocalDateTime endTime = LocalDateTime.now().plusMinutes(request.getDurationInMinutes());
        auction.setEndTime(endTime);

        auction.setStatus(AuctionStatus.DRAFT);

        return auctionRepository.save(auction);
    }

    public String placeBid(String userId, Long auctionId, Double amount) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction tidak ditemukan"));

        if (!auction.getStatus().equals(AuctionStatus.ACTIVE) &&
                !auction.getStatus().equals(AuctionStatus.EXTENDED)) {
            return "Auction tidak dalam status aktif";
        }

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

        return "Bid berhasil, saldo ditahan";
    }

    public String winAuction(String userId, Double amount) {

        walletService.deductHeldBalance(userId, amount);

        return "User menang lelang, saldo dipotong";
    }

    public String loseAuction(String userId, Double amount) {

        walletService.releaseHeldBalance(userId, amount);

        return "User kalah lelang, saldo dikembalikan";
    }
}