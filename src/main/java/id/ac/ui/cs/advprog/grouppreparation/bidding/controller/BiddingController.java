package id.ac.ui.cs.advprog.grouppreparation.bidding.controller;

import id.ac.ui.cs.advprog.grouppreparation.bidding.model.Auction;
import id.ac.ui.cs.advprog.grouppreparation.bidding.service.BiddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bidding")
@CrossOrigin(origins = "*")
public class BiddingController {

    @Autowired
    private BiddingService biddingService;

    @GetMapping("/dummy")
    public List<Auction> getDummyAuctions() {
        return biddingService.getDummyAuctions();
    }

    @PostMapping("/dummy")
    public String createDummyAuction() {
        return biddingService.createDummyAuction();
    }
}