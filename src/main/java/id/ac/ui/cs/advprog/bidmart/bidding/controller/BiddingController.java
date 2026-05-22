package id.ac.ui.cs.advprog.bidmart.bidding.controller;

import id.ac.ui.cs.advprog.bidmart.bidding.dto.CreateAuctionRequest;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.service.BiddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bidding")
@CrossOrigin(origins = "*")
public class BiddingController {

    @Autowired
    private BiddingService biddingService;

    @Autowired
    private AuctionRepository auctionRepository;

    @PostMapping("/create")
    public ResponseEntity<Auction> createAuction(@RequestBody CreateAuctionRequest request) {
        Auction newAuction = biddingService.createAuction(request);
        return ResponseEntity.ok(newAuction);
    }

    @PostMapping("/bid")
    public String placeBid(
            @RequestParam String userId,
            @RequestParam Long auctionId,
            @RequestParam Double amount
    ) {
        return biddingService.placeBid(userId, auctionId, amount);
    }

    @GetMapping("/auctions")
    public ResponseEntity<List<Auction>> getAllAuctions() {
        return ResponseEntity.ok(auctionRepository.findAll());
    }

    @GetMapping("/auctions/{id}")
    public ResponseEntity<Auction> getAuction(@PathVariable Long id) {
        return ResponseEntity.ok(auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction tidak ditemukan")));
    }
}