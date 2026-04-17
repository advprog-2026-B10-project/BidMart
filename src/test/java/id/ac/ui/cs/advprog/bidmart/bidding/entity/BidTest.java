package id.ac.ui.cs.advprog.bidmart.bidding.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidTest {

    @Test
    void bid_setterAndGetter_shouldWorkCorrectly() {
        Bid bid = new Bid();
        Auction auction = new Auction();
        LocalDateTime timestamp = LocalDateTime.now();

        bid.setId(1L);
        bid.setBuyerId("user123");
        bid.setAmount(75000.0);
        bid.setTimestamp(timestamp);
        bid.setAuction(auction);

        assertEquals(1L, bid.getId());
        assertEquals("user123", bid.getBuyerId());
        assertEquals(75000.0, bid.getAmount());
        assertEquals(timestamp, bid.getTimestamp());
        assertEquals(auction, bid.getAuction());
    }

    @Test
    void bid_noArgsConstructor_shouldCreateEmptyObject() {
        Bid bid = new Bid();
        assertNull(bid.getId());
        assertNull(bid.getBuyerId());
        assertNull(bid.getAmount());
        assertNull(bid.getTimestamp());
        assertNull(bid.getAuction());
    }

    @Test
    void bid_buyerId_shouldAcceptStringFormat() {
        Bid bid = new Bid();

        bid.setBuyerId("user-001");
        assertEquals("user-001", bid.getBuyerId());

        bid.setBuyerId("admin@bidmart.com");
        assertEquals("admin@bidmart.com", bid.getBuyerId());
    }

    @Test
    void bid_amount_shouldAcceptDoubleValues() {
        Bid bid = new Bid();

        bid.setAmount(50000.0);
        assertEquals(50000.0, bid.getAmount());

        bid.setAmount(99999999.99);
        assertEquals(99999999.99, bid.getAmount());
    }

    @Test
    void bid_timestamp_shouldBeSettable() {
        Bid bid = new Bid();
        LocalDateTime now = LocalDateTime.now();

        bid.setTimestamp(now);
        assertEquals(now, bid.getTimestamp());
    }
}
