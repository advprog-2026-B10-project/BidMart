package id.ac.ui.cs.advprog.bidmart.bidding.service;

import id.ac.ui.cs.advprog.bidmart.bidding.dto.CreateAuctionRequest;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionType;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.strategy.AuctionStrategy;
import id.ac.ui.cs.advprog.bidmart.bidding.strategy.AuctionStrategyFactory;
import jakarta.persistence.OptimisticLockException;
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
    private AuctionStrategyFactory strategyFactory;

    @Mock
    private AuctionStrategy auctionStrategy;

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
        activeAuction.setType(AuctionType.ENGLISH);
        activeAuction.setBids(new ArrayList<>());
    }

    // ===== createAuction =====

    @Test
    void createAuction_shouldReturnSavedAuctionWithDraftStatus() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setListingId(100L);
        request.setStartingPrice(50000.0);
        request.setReservePrice(100000.0);
        request.setDurationInMinutes(60);
        request.setSellerId("seller@bidmart.com");

        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        Auction result = biddingService.createAuction(request);

        assertNotNull(result);
        assertEquals(AuctionStatus.DRAFT, result.getStatus());
        assertEquals(AuctionType.ENGLISH, result.getType()); // default ENGLISH
        assertEquals(100L, result.getListingId());
        verify(auctionRepository, times(1)).save(any(Auction.class));
    }

    @Test
    void createAuction_withExplicitType_shouldUseProvidedType() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setListingId(100L);
        request.setStartingPrice(50000.0);
        request.setReservePrice(100000.0);
        request.setDurationInMinutes(60);
        request.setSellerId("seller@bidmart.com");
        request.setType(AuctionType.SCHOLARSHIP);

        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        Auction result = biddingService.createAuction(request);

        assertEquals(AuctionType.SCHOLARSHIP, result.getType());
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
        verify(strategyFactory, never()).getStrategy(any());
    }

    @Test
    void placeBid_auctionClosed_shouldReturnErrorMessage() {
        activeAuction.setStatus(AuctionStatus.CLOSED);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));

        String result = biddingService.placeBid("user1", 1L, 60000.0);

        assertEquals("Auction tidak dalam status aktif", result);
    }

    @Test
    void placeBid_activeAuction_shouldDelegateToStrategy() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));
        when(strategyFactory.getStrategy(AuctionType.ENGLISH)).thenReturn(auctionStrategy);
        when(auctionStrategy.placeBid(activeAuction, "user1", 60000.0)).thenReturn("Bid berhasil, saldo ditahan");

        String result = biddingService.placeBid("user1", 1L, 60000.0);

        assertEquals("Bid berhasil, saldo ditahan", result);
        verify(strategyFactory).getStrategy(AuctionType.ENGLISH);
        verify(auctionStrategy).placeBid(activeAuction, "user1", 60000.0);
    }

    @Test
    void placeBid_extendedAuction_shouldDelegateToStrategy() {
        activeAuction.setStatus(AuctionStatus.EXTENDED);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));
        when(strategyFactory.getStrategy(AuctionType.ENGLISH)).thenReturn(auctionStrategy);
        when(auctionStrategy.placeBid(activeAuction, "user1", 60000.0)).thenReturn("Bid berhasil, saldo ditahan");

        String result = biddingService.placeBid("user1", 1L, 60000.0);

        assertEquals("Bid berhasil, saldo ditahan", result);
        verify(auctionStrategy).placeBid(activeAuction, "user1", 60000.0);
    }

    @Test
    void placeBid_optimisticLockException_shouldReturnBusyMessage() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(activeAuction));
        when(strategyFactory.getStrategy(any())).thenReturn(auctionStrategy);
        when(auctionStrategy.placeBid(any(), any(), any())).thenThrow(OptimisticLockException.class);

        String result = biddingService.placeBid("user1", 1L, 60000.0);

        assertEquals("Sistem sedang sibuk, silakan coba lagi", result);
    }

    // ===== determineWinner =====

    @Test
    void determineWinner_shouldDelegateToStrategy() {
        when(strategyFactory.getStrategy(AuctionType.ENGLISH)).thenReturn(auctionStrategy);
        when(auctionStrategy.determineWinner(activeAuction)).thenReturn("User menang lelang, saldo dipotong");

        String result = biddingService.determineWinner(activeAuction);

        assertEquals("User menang lelang, saldo dipotong", result);
        verify(strategyFactory).getStrategy(AuctionType.ENGLISH);
        verify(auctionStrategy).determineWinner(activeAuction);
    }

    // ===== closeExpiredAuctions =====

    @Test
    void closeExpiredAuctions_expiredAuction_shouldCloseAndDetermineWinner() {
        activeAuction.setEndTime(LocalDateTime.now().minusMinutes(5));
        when(auctionRepository.findByStatusIn(anyList())).thenReturn(List.of(activeAuction));
        when(strategyFactory.getStrategy(any())).thenReturn(auctionStrategy);
        when(auctionStrategy.determineWinner(any())).thenReturn("Lelang berakhir tanpa pemenang");

        biddingService.closeExpiredAuctions();

        assertEquals(AuctionStatus.CLOSED, activeAuction.getStatus());
        verify(auctionStrategy).determineWinner(activeAuction);
    }

    @Test
    void closeExpiredAuctions_notExpiredAuction_shouldNotClose() {
        activeAuction.setEndTime(LocalDateTime.now().plusHours(1));
        when(auctionRepository.findByStatusIn(anyList())).thenReturn(List.of(activeAuction));

        biddingService.closeExpiredAuctions();

        assertEquals(AuctionStatus.ACTIVE, activeAuction.getStatus());
        verify(auctionStrategy, never()).determineWinner(any());
    }

    @Test
    void closeExpiredAuctions_noActiveAuctions_shouldDoNothing() {
        when(auctionRepository.findByStatusIn(anyList())).thenReturn(List.of());

        biddingService.closeExpiredAuctions();

        verify(auctionStrategy, never()).determineWinner(any());
    }
}