package id.ac.ui.cs.advprog.bidmart.bidding.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAuctionRequest {
    private long listingId;
    private Double startingPrice;
    private Double reservePrice;
    private Integer durationInMinutes;
}
