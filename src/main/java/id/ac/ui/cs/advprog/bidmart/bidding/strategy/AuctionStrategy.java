package id.ac.ui.cs.advprog.bidmart.bidding.strategy;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;

public interface AuctionStrategy {
    String placeBid(Auction auction, String userId, Double amount);
    String determineWinner(Auction auction);
}