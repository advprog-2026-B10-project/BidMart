package id.ac.ui.cs.advprog.bidmart.bidding.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    @Test
    void auction_setterAndGetter_shouldWorkCorrectly() {
        Auction auction = new Auction();
        LocalDateTime endTime = LocalDateTime.now().plusHours(1);

        auction.setId(1L);
        auction.setListingId(100L);
        auction.setStartingPrice(50000.0);
        auction.setReservePrice(100000.0);
        auction.setEndTime(endTime);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setBids(new ArrayList<>());

        assertEquals(1L, auction.getId());
        assertEquals(100L, auction.getListingId());
        assertEquals(50000.0, auction.getStartingPrice());
        assertEquals(100000.0, auction.getReservePrice());
        assertEquals(endTime, auction.getEndTime());
        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        assertNotNull(auction.getBids());
        assertTrue(auction.getBids().isEmpty());
    }

    @Test
    void auction_noArgsConstructor_shouldCreateEmptyObject() {
        Auction auction = new Auction();
        assertNull(auction.getId());
        assertNull(auction.getListingId());
        assertNull(auction.getStatus());
    }

    @Test
    void auction_statusTransitions_shouldSetCorrectly() {
        Auction auction = new Auction();

        auction.setStatus(AuctionStatus.DRAFT);
        assertEquals(AuctionStatus.DRAFT, auction.getStatus());

        auction.setStatus(AuctionStatus.ACTIVE);
        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());

        auction.setStatus(AuctionStatus.EXTENDED);
        assertEquals(AuctionStatus.EXTENDED, auction.getStatus());

        auction.setStatus(AuctionStatus.CLOSED);
        assertEquals(AuctionStatus.CLOSED, auction.getStatus());

        auction.setStatus(AuctionStatus.WON);
        assertEquals(AuctionStatus.WON, auction.getStatus());

        auction.setStatus(AuctionStatus.UNSOLD);
        assertEquals(AuctionStatus.UNSOLD, auction.getStatus());
    }

    @Test
    void auction_withBids_shouldStoreBidsCorrectly() {
        Auction auction = new Auction();
        List<Bid> bids = new ArrayList<>();

        Bid bid1 = new Bid();
        bid1.setAmount(60000.0);
        Bid bid2 = new Bid();
        bid2.setAmount(70000.0);
        bids.add(bid1);
        bids.add(bid2);

        auction.setBids(bids);

        assertEquals(2, auction.getBids().size());
        assertEquals(60000.0, auction.getBids().get(0).getAmount());
        assertEquals(70000.0, auction.getBids().get(1).getAmount());
    }

    @Test
    void auction_endTime_shouldBeUpdatable() {
        Auction auction = new Auction();
        LocalDateTime original = LocalDateTime.now().plusHours(1);
        LocalDateTime extended = original.plusMinutes(2);

        auction.setEndTime(original);
        assertEquals(original, auction.getEndTime());

        auction.setEndTime(extended);
        assertEquals(extended, auction.getEndTime());
    }
}