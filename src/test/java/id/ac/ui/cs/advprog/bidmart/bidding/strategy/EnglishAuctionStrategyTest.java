package id.ac.ui.cs.advprog.bidmart.bidding.strategy;

import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Bid;
import id.ac.ui.cs.advprog.bidmart.bidding.event.AuctionEndedEvent;
import id.ac.ui.cs.advprog.bidmart.bidding.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.BidRepository;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnglishAuctionStrategyTest {

    @Mock
    private WalletService walletService;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EnglishAuctionStrategy strategy;

    private Auction auction;

    @BeforeEach
    void setUp() {
        auction = new Auction();
        auction.setId(1L);
        auction.setStartingPrice(50000.0);
        auction.setReservePrice(100000.0);
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setBids(new ArrayList<>());
    }

    // ===== placeBid =====

    @Test
    void placeBid_auctionExpired_shouldReturnErrorMessage() {
        auction.setEndTime(LocalDateTime.now().minusMinutes(5));

        String result = strategy.placeBid(auction, "user1", 60000.0);

        assertEquals("Auction sudah berakhir", result);
        verifyNoInteractions(walletService);
    }

    @Test
    void placeBid_amountTooLow_shouldReturnErrorMessage() {
        String result = strategy.placeBid(auction, "user1", 30000.0);

        assertEquals("Bid harus lebih tinggi dari penawaran tertinggi saat ini", result);
        verifyNoInteractions(walletService);
    }

    @Test
    void placeBid_amountEqualToStartingPrice_shouldReturnErrorMessage() {
        String result = strategy.placeBid(auction, "user1", 50000.0);

        assertEquals("Bid harus lebih tinggi dari penawaran tertinggi saat ini", result);
    }

    @Test
    void placeBid_validBid_noPreviousBids_shouldSucceed() {
        when(bidRepository.save(any(Bid.class))).thenReturn(new Bid());
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        String result = strategy.placeBid(auction, "user1", 60000.0);

        assertEquals("Bid berhasil, saldo ditahan", result);
        verify(walletService).holdBalance("user1", 60000.0);
        verify(bidRepository).save(any(Bid.class));
        verify(eventPublisher).publishEvent(any(BidPlacedEvent.class));
    }

    @Test
    void placeBid_validBid_shouldReleasePreviousHighestBid() {
        Bid existingBid = new Bid();
        existingBid.setBuyerId("user1");
        existingBid.setAmount(60000.0);
        existingBid.setTimestamp(LocalDateTime.now());
        auction.getBids().add(existingBid);

        when(bidRepository.save(any(Bid.class))).thenReturn(new Bid());
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        String result = strategy.placeBid(auction, "user2", 70000.0);

        assertEquals("Bid berhasil, saldo ditahan", result);
        verify(walletService).releaseHeldBalance("user1", 60000.0);
        verify(walletService).holdBalance("user2", 70000.0);
    }

    @Test
    void placeBid_inLastTwoMinutes_shouldExtendAuction() {
        auction.setEndTime(LocalDateTime.now().plusMinutes(1));
        when(bidRepository.save(any(Bid.class))).thenReturn(new Bid());
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        strategy.placeBid(auction, "user1", 60000.0);

        assertEquals(AuctionStatus.EXTENDED, auction.getStatus());
    }

    @Test
    void placeBid_notInLastTwoMinutes_shouldNotExtendAuction() {
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        when(bidRepository.save(any(Bid.class))).thenReturn(new Bid());
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        strategy.placeBid(auction, "user1", 60000.0);

        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
    }

    @Test
    void placeBid_shouldSaveBidWithCorrectData() {
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArgument(0));
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        strategy.placeBid(auction, "user1", 60000.0);

        verify(bidRepository).save(argThat(bid ->
                bid.getBuyerId().equals("user1") &&
                        bid.getAmount().equals(60000.0) &&
                        bid.getAuction().equals(auction) &&
                        bid.getTimestamp() != null
        ));
    }

    // ===== determineWinner =====

    @Test
    void determineWinner_noBids_shouldSetUnsold() {
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        String result = strategy.determineWinner(auction);

        assertEquals(AuctionStatus.UNSOLD, auction.getStatus());
        assertEquals("Lelang berakhir tanpa pemenang", result);
        verify(eventPublisher).publishEvent(any(AuctionEndedEvent.class));
        verifyNoInteractions(walletService);
    }

    @Test
    void determineWinner_highestBidMeetsReservePrice_shouldSetWon() {
        Bid bid = new Bid();
        bid.setBuyerId("user1");
        bid.setAmount(150000.0);
        bid.setTimestamp(LocalDateTime.now());
        auction.getBids().add(bid);
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        String result = strategy.determineWinner(auction);

        assertEquals(AuctionStatus.WON, auction.getStatus());
        verify(walletService).deductHeldBalance("user1", 150000.0);
        verify(eventPublisher).publishEvent(any(AuctionEndedEvent.class));
    }

    @Test
    void determineWinner_highestBidEqualsReservePrice_shouldSetWon() {
        Bid bid = new Bid();
        bid.setBuyerId("user1");
        bid.setAmount(100000.0);
        bid.setTimestamp(LocalDateTime.now());
        auction.getBids().add(bid);
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        strategy.determineWinner(auction);

        assertEquals(AuctionStatus.WON, auction.getStatus());
    }

    @Test
    void determineWinner_highestBidBelowReservePrice_shouldSetUnsold() {
        Bid bid = new Bid();
        bid.setBuyerId("user1");
        bid.setAmount(80000.0);
        bid.setTimestamp(LocalDateTime.now());
        auction.getBids().add(bid);
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        strategy.determineWinner(auction);

        assertEquals(AuctionStatus.UNSOLD, auction.getStatus());
        verify(walletService).releaseHeldBalance("user1", 80000.0);
        verify(walletService, never()).deductHeldBalance(any(), any());
    }

    @Test
    void determineWinner_multipleBids_shouldPickHighest() {
        Bid bid1 = new Bid();
        bid1.setBuyerId("user1");
        bid1.setAmount(80000.0);
        bid1.setTimestamp(LocalDateTime.now());

        Bid bid2 = new Bid();
        bid2.setBuyerId("user2");
        bid2.setAmount(150000.0);
        bid2.setTimestamp(LocalDateTime.now());

        auction.getBids().add(bid1);
        auction.getBids().add(bid2);
        when(auctionRepository.save(any(Auction.class))).thenReturn(auction);

        strategy.determineWinner(auction);

        assertEquals(AuctionStatus.WON, auction.getStatus());
        verify(walletService).deductHeldBalance("user2", 150000.0);
        verify(walletService, never()).deductHeldBalance("user1", 80000.0);
    }
}