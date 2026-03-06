package id.ac.ui.cs.advprog.bidmart.bidding.service;

import id.ac.ui.cs.advprog.bidmart.bidding.dto.CreateAuctionRequest;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BiddingService {

    @Autowired
    private AuctionRepository auctionRepository;

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
}
