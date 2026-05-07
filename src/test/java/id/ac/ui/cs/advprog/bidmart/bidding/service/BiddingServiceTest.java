package id.ac.ui.cs.advprog.bidmart.bidding.service;

import id.ac.ui.cs.advprog.bidmart.bidding.dto.CreateAuctionRequest;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Bid;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.BidRepository;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private BiddingService biddingService;

    private Auction activeAuction;

    @BeforeEach
    void setUp() {
        activeAuction = new Auction();
        activeAuction.setId(1L);
        activeAuction.setListingId(100L);
        activeAuction.setStartingPrice(50000.0);
        activeAuction.setReservePrice(100000.0);
        activeAuction.setEndTime(LocalDateTime.now().plusHours(1));
        activeAuction.setStatus(AuctionStatus.ACTIVE);
        activeAuction.setBids(new ArrayList<>());
    }

    // ===== createAuction =====

    @Test
    void createAuction_shouldReturnSavedAuction() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setListingId(100L);
        request.setStartingPrice(50000.0);
        request.setReservePrice(100000.0);
        request.setDurationInMinutes(60);

        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        Auction result = biddingService.createAuction(request);

        assertNotNull(result);
        assertEquals(AuctionStatus.DRAFT, result.getStatus());
        assertEquals(100L, result.getListingId());
        verify(auctionRepository, times(1)).save(any(Auction.class));
    }

    // ===== placeBid =====

    @Test
    void placeBid_auctionNotFound_shouldThrowException() {
        when(auctionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                biddingService.placeBid("user1", 99L, 60000.0));
    }

    @Test
    void placeBid_auctionNotActive_shouldReturnErrorMessage() {
        activeAuction.setStatus(AuctionStatus.DRAFT);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));

        String result = biddingService.placeBid("user1", 1L, 60000.0);

        assertEquals("Auction tidak dalam status aktif", result);
    }

    @Test
    void placeBid_auctionExpired_shouldReturnErrorMessage() {
        activeAuction.setEndTime(LocalDateTime.now().minusMinutes(5));
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));

        String result = biddingService.placeBid("user1", 1L, 60000.0);

        assertEquals("Auction sudah berakhir", result);
    }

    @Test
    void placeBid_amountTooLow_shouldReturnErrorMessage() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));

        // amount lebih rendah dari startingPrice
        String result = biddingService.placeBid("user1", 1L, 30000.0);

        assertEquals("Bid harus lebih tinggi dari penawaran tertinggi saat ini", result);
    }

    @Test
    void placeBid_amountEqualToStartingPrice_shouldReturnErrorMessage() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));

        String result = biddingService.placeBid("user1", 1L, 50000.0);

        assertEquals("Bid harus lebih tinggi dari penawaran tertinggi saat ini", result);
    }

    @Test
    void placeBid_validBid_noPreviousBids_shouldSucceed() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));
        when(bidRepository.save(any(Bid.class))).thenReturn(new Bid());
        when(auctionRepository.save(any(Auction.class))).thenReturn(activeAuction);

        String result = biddingService.placeBid("user1", 1L, 60000.0);

        assertEquals("Bid berhasil, saldo ditahan", result);
        verify(walletService).holdBalance("user1", 60000.0);
        verify(bidRepository).save(any(Bid.class));
    }

    @Test
    void placeBid_validBid_shouldReleasePreviousHighestBid() {
        Bid existingBid = new Bid();
        existingBid.setBuyerId("user1");
        existingBid.setAmount(60000.0);
        existingBid.setTimestamp(LocalDateTime.now());
        activeAuction.getBids().add(existingBid);

        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));
        when(bidRepository.save(any(Bid.class))).thenReturn(new Bid());
        when(auctionRepository.save(any(Auction.class))).thenReturn(activeAuction);

        String result = biddingService.placeBid("user2", 1L, 70000.0);

        assertEquals("Bid berhasil, saldo ditahan", result);
        verify(walletService).releaseHeldBalance("user1", 60000.0);
        verify(walletService).holdBalance("user2", 70000.0);
    }

    @Test
    void placeBid_inLastTwoMinutes_shouldExtendAuction() {
        activeAuction.setEndTime(LocalDateTime.now().plusMinutes(1)); // 1 menit lagi habis
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));
        when(bidRepository.save(any(Bid.class))).thenReturn(new Bid());
        when(auctionRepository.save(any(Auction.class))).thenReturn(activeAuction);

        biddingService.placeBid("user1", 1L, 60000.0);

        assertEquals(AuctionStatus.EXTENDED, activeAuction.getStatus());
    }

    @Test
    void placeBid_notInLastTwoMinutes_shouldNotExtendAuction() {
        activeAuction.setEndTime(LocalDateTime.now().plusHours(1)); // masih lama
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));
        when(bidRepository.save(any(Bid.class))).thenReturn(new Bid());
        when(auctionRepository.save(any(Auction.class))).thenReturn(activeAuction);

        biddingService.placeBid("user1", 1L, 60000.0);

        assertEquals(AuctionStatus.ACTIVE, activeAuction.getStatus());
    }

    @Test
    void placeBid_extendedAuction_shouldStillAcceptBid() {
        activeAuction.setStatus(AuctionStatus.EXTENDED);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));
        when(bidRepository.save(any(Bid.class))).thenReturn(new Bid());
        when(auctionRepository.save(any(Auction.class))).thenReturn(activeAuction);

        String result = biddingService.placeBid("user1", 1L, 60000.0);

        assertEquals("Bid berhasil, saldo ditahan", result);
    }

    // ===== determineWinner =====

    @Test
    void determineWinner_noBids_shouldSetUnsold() {
        when(auctionRepository.save(any(Auction.class))).thenReturn(activeAuction);

        biddingService.determineWinner(activeAuction);

        assertEquals(AuctionStatus.UNSOLD, activeAuction.getStatus());
        verify(auctionRepository).save(activeAuction);
    }

    @Test
    void determineWinner_highestBidMeetsReservePrice_shouldSetWon() {
        Bid bid = new Bid();
        bid.setBuyerId("user1");
        bid.setAmount(150000.0); // >= reservePrice (100000)
        bid.setTimestamp(LocalDateTime.now());
        activeAuction.getBids().add(bid);

        when(auctionRepository.save(any(Auction.class))).thenReturn(activeAuction);

        biddingService.determineWinner(activeAuction);

        assertEquals(AuctionStatus.WON, activeAuction.getStatus());
        verify(walletService).deductHeldBalance("user1", 150000.0);
    }

    @Test
    void determineWinner_highestBidEqualsReservePrice_shouldSetWon() {
        Bid bid = new Bid();
        bid.setBuyerId("user1");
        bid.setAmount(100000.0); // == reservePrice
        bid.setTimestamp(LocalDateTime.now());
        activeAuction.getBids().add(bid);

        when(auctionRepository.save(any(Auction.class))).thenReturn(activeAuction);

        biddingService.determineWinner(activeAuction);

        assertEquals(AuctionStatus.WON, activeAuction.getStatus());
    }

    @Test
    void determineWinner_highestBidBelowReservePrice_shouldSetUnsold() {
        Bid bid = new Bid();
        bid.setBuyerId("user1");
        bid.setAmount(80000.0); // < reservePrice (100000)
        bid.setTimestamp(LocalDateTime.now());
        activeAuction.getBids().add(bid);

        when(auctionRepository.save(any(Auction.class))).thenReturn(activeAuction);

        biddingService.determineWinner(activeAuction);

        assertEquals(AuctionStatus.UNSOLD, activeAuction.getStatus());
        verify(walletService).releaseHeldBalance("user1", 80000.0);
    }

    // ===== closeExpiredAuctions =====

    @Test
    void closeExpiredAuctions_expiredAuction_shouldCloseAndDetermineWinner() {
        activeAuction.setEndTime(LocalDateTime.now().minusMinutes(5));

        when(auctionRepository.findByStatusIn(anyList())).thenReturn(List.of(activeAuction));
        when(auctionRepository.save(any(Auction.class))).thenReturn(activeAuction);

        biddingService.closeExpiredAuctions();

        assertEquals(AuctionStatus.UNSOLD, activeAuction.getStatus()); // ga ada bid → UNSOLD
    }

    @Test
    void closeExpiredAuctions_notExpiredAuction_shouldNotClose() {
        activeAuction.setEndTime(LocalDateTime.now().plusHours(1));

        when(auctionRepository.findByStatusIn(anyList())).thenReturn(List.of(activeAuction));

        biddingService.closeExpiredAuctions();

        assertEquals(AuctionStatus.ACTIVE, activeAuction.getStatus());
        verify(auctionRepository, never()).save(any());
    }

    @Test
    void closeExpiredAuctions_noActiveAuctions_shouldDoNothing() {
        when(auctionRepository.findByStatusIn(anyList())).thenReturn(List.of());

        biddingService.closeExpiredAuctions();

        verify(auctionRepository, never()).save(any());
    }
}