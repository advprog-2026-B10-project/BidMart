package id.ac.ui.cs.advprog.bidmart.bidding.controller;

import id.ac.ui.cs.advprog.bidmart.bidding.dto.CreateAuctionRequest;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.service.BiddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bidding")
@CrossOrigin(origins = "*")
public class BiddingController {

    @Autowired
    private BiddingService biddingService;

    @PostMapping("/create")
    public ResponseEntity<Auction> createAuction(@RequestBody CreateAuctionRequest request) {
        Auction newAuction = biddingService.createAuction(request);
        return ResponseEntity.ok(newAuction);
    }
}
