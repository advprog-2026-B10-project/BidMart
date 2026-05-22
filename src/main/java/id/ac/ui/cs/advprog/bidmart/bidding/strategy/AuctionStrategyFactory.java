package id.ac.ui.cs.advprog.bidmart.bidding.strategy;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuctionStrategyFactory {

    @Autowired
    private EnglishAuctionStrategy englishAuctionStrategy;

    public AuctionStrategy getStrategy(AuctionType type) {
        return switch (type) {
            case ENGLISH -> englishAuctionStrategy;
            default -> englishAuctionStrategy; // default fallback
        };
    }
}