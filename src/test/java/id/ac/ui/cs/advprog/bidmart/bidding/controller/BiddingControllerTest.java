package id.ac.ui.cs.advprog.bidmart.bidding.controller;

import id.ac.ui.cs.advprog.bidmart.bidding.dto.CreateAuctionRequest;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.entity.AuctionType;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.service.BiddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingControllerTest {

    @Mock
    private BiddingService biddingService;

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private BiddingController biddingController;

    private Auction auction;

    @BeforeEach
    void setUp() {
        auction = new Auction();
        auction.setId(1L);
        auction.setListingId(100L);
        auction.setStartingPrice(50000.0);
        auction.setReservePrice(100000.0);
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setType(AuctionType.ENGLISH);
        auction.setSellerId("seller@bidmart.com");
        auction.setBids(new ArrayList<>());
    }

    // ===== createAuction =====

    @Test
    void createAuction_shouldReturn200WithAuction() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setListingId(100L);
        request.setStartingPrice(50000.0);
        request.setReservePrice(100000.0);
        request.setDurationInMinutes(60);
        request.setSellerId("seller@bidmart.com");

        when(biddingService.createAuction(any(CreateAuctionRequest.class))).thenReturn(auction);

        ResponseEntity<Auction> response = biddingController.createAuction(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        verify(biddingService).createAuction(any(CreateAuctionRequest.class));
    }

    // ===== placeBid =====

    @Test
    void placeBid_shouldReturnSuccessMessage() {
        when(biddingService.placeBid("user1", 1L, 60000.0)).thenReturn("Bid berhasil, saldo ditahan");

        String result = biddingController.placeBid("user1", 1L, 60000.0);

        assertEquals("Bid berhasil, saldo ditahan", result);
        verify(biddingService).placeBid("user1", 1L, 60000.0);
    }

    @Test
    void placeBid_auctionNotActive_shouldReturnErrorMessage() {
        when(biddingService.placeBid("user1", 1L, 60000.0)).thenReturn("Auction tidak dalam status aktif");

        String result = biddingController.placeBid("user1", 1L, 60000.0);

        assertEquals("Auction tidak dalam status aktif", result);
    }

    @Test
    void placeBid_amountTooLow_shouldReturnErrorMessage() {
        when(biddingService.placeBid("user1", 1L, 30000.0)).thenReturn("Bid harus lebih tinggi dari penawaran tertinggi saat ini");

        String result = biddingController.placeBid("user1", 1L, 30000.0);

        assertEquals("Bid harus lebih tinggi dari penawaran tertinggi saat ini", result);
    }

    @Test
    void placeBid_systemBusy_shouldReturnBusyMessage() {
        when(biddingService.placeBid("user1", 1L, 60000.0)).thenReturn("Sistem sedang sibuk, silakan coba lagi");

        String result = biddingController.placeBid("user1", 1L, 60000.0);

        assertEquals("Sistem sedang sibuk, silakan coba lagi", result);
    }

    // ===== getAllAuctions =====

    @Test
    void getAllAuctions_shouldReturn200WithList() {
        when(auctionRepository.findAll()).thenReturn(List.of(auction));

        ResponseEntity<List<Auction>> response = biddingController.getAllAuctions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).getId());
    }

    @Test
    void getAllAuctions_empty_shouldReturn200WithEmptyList() {
        when(auctionRepository.findAll()).thenReturn(List.of());

        ResponseEntity<List<Auction>> response = biddingController.getAllAuctions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    // ===== getAuction =====

    @Test
    void getAuction_found_shouldReturn200WithAuction() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));

        ResponseEntity<Auction> response = biddingController.getAuction(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    void getAuction_notFound_shouldThrowException() {
        when(auctionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> biddingController.getAuction(99L));
    }
}