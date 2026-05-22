package id.ac.ui.cs.advprog.bidmart.catalog.repository;

import id.ac.ui.cs.advprog.bidmart.catalog.entity.Catalog;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Bid;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class CatalogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CatalogRepository catalogRepository;

    @Test
    void testHasBidsReturnsTrue() {
        Catalog catalog = new Catalog("Test", "Desc", "img", null, 100.0, 200.0, 60, "seller1");
        catalog = entityManager.persistAndFlush(catalog);

        Auction auction = new Auction();
        auction.setListingId(catalog.getId());
        auction.setStartingPrice(100.0);
        auction.setReservePrice(200.0);
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setSellerId("seller1");
        auction = entityManager.persistAndFlush(auction);

        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setAmount(150.0);
        bid.setBuyerId("buyer1");
        bid.setTimestamp(LocalDateTime.now());
        entityManager.persistAndFlush(bid);

        assertTrue(catalogRepository.hasBids(catalog.getId()));
    }

    @Test
    void testHasBidsReturnsFalseNoBids() {
        Catalog catalog = new Catalog("Test3", "Desc", "img", null, 100.0, 200.0, 60, "seller1");
        catalog = catalogRepository.save(catalog);

        Auction auction = new Auction();
        auction.setListingId(catalog.getId());
        auction.setStartingPrice(100.0);
        auction.setReservePrice(200.0);
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setSellerId("seller1");
        entityManager.persistAndFlush(auction);

        boolean hasBids = catalogRepository.hasBids(catalog.getId());
        assertFalse(hasBids);
    }

    @Test
    void testSearchKatalogWithRealDatabase() {
        Catalog catalog = new Catalog("Iphone 13 Pro", "Mulus", "img", null, 5000.0, 6000.0, 60, "seller1");
        catalog = catalogRepository.save(catalog);

        id.ac.ui.cs.advprog.bidmart.catalog.service.CatalogAuthService authServiceMock = org.mockito.Mockito.mock(id.ac.ui.cs.advprog.bidmart.catalog.service.CatalogAuthService.class);
        id.ac.ui.cs.advprog.bidmart.catalog.service.CatalogService catalogService = new id.ac.ui.cs.advprog.bidmart.catalog.service.CatalogService(catalogRepository, authServiceMock);

        java.util.List<Catalog> results = catalogService.searchKatalog("Iphone", null, 4000.0, 6000.0, LocalDateTime.now().plusDays(2));
        
        // Results will be 0 because we didn't add an auction to this DB, but this perfectly covers the JPA lambda block execution!
        assertEquals(0, results.size());
        
        // Also test the search without endTime to cover both branches
        java.util.List<Catalog> resultsNoTime = catalogService.searchKatalog("Iphone", null, 4000.0, 6000.0, null);
        assertEquals(1, resultsNoTime.size());
    }

    @Test
    void testHasBidsReturnsFalseNoAuction() {
        Catalog catalog = new Catalog("Test", "Desc", "img", null, 100.0, 200.0, 60, "seller1");
        catalog = entityManager.persistAndFlush(catalog);

        assertFalse(catalogRepository.hasBids(catalog.getId()));
    }
}
