package id.ac.ui.cs.advprog.grouppreparation.bidding.service;

import id.ac.ui.cs.advprog.grouppreparation.bidding.model.Auction;
import id.ac.ui.cs.advprog.grouppreparation.bidding.repository.AuctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BiddingService {

    @Autowired
    private AuctionRepository auctionRepository;

    public List<Auction> getDummyAuctions() {
        return auctionRepository.findAll();
    }

    public String createDummyAuction() {
        Auction dummy = new Auction();
        dummy.setTitle("Lelang Sepatu Futsal Dummy");
        dummy.setStartingPrice(150000.0);
        auctionRepository.save(dummy);
        return "Data dummy ditambahkan";
    }
}